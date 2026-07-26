package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.dto.LeaveApprovalRequest;
import com.app.leaveManagement.dto.LeaveApprovalResponse;
import com.app.leaveManagement.entity.LeaveApproval;
import com.app.leaveManagement.entity.LeaveApplication;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.ApprovalStatus;
import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.event.LeaveStatusChangedEvent;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveApprovalRepository;
import com.app.leaveManagement.repository.LeaveApplicationRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveApprovalService;
import com.app.leaveManagement.service.LeaveBalanceService;
import com.app.leaveManagement.service.LeaveStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveApprovalServiceImpl implements LeaveApprovalService {

    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveStateMachine leaveStateMachine;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @Auditable(action = "MANAGER_DECISION", entityType = "LeaveApproval")
    public LeaveApprovalResponse processManagerDecision(
            Long managerId,
            Long leaveApplicationId,
            LeaveApprovalRequest request) {

        log.info("Manager id: {} processing decision for leave id: {}",
                managerId, leaveApplicationId);

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Manager not found with id: " + managerId
                ));

        LeaveApplication application = leaveApplicationRepository
                .findById(leaveApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave application not found with id: " + leaveApplicationId
                ));

        // Manager can only act on their direct reports' leaves
        if (application.getUser().getManager() == null ||
                !application.getUser().getManager().getId().equals(managerId)) {
            log.warn("Manager id: {} attempted to action leave not belonging to their team",
                    managerId);
            throw new InvalidStateTransitionException(
                "You can only action leave applications of your direct reports"
            );
        }

        // Leave must be in PENDING state for manager action
        if (application.getStatus() != LeaveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Leave application is not in PENDING state. Current status: "
                    + application.getStatus()
            );
        }

        // Check if manager already actioned this
        leaveApprovalRepository
            .findByLeaveApplicationIdAndApprovalLevel(leaveApplicationId, 1)
            .ifPresent(existing -> {
                throw new InvalidStateTransitionException(
                    "Manager has already actioned this leave application"
                );
            });

        LeaveStatus previousStatus = application.getStatus();
        LeaveStatus newStatus;
        ApprovalStatus approvalStatus;

        if (request.getDecision() == ApprovalStatus.APPROVED) {
            leaveStateMachine.validateTransition(application.getStatus(), LeaveStatus.APPROVED);
            newStatus = LeaveStatus.APPROVED;
            approvalStatus = ApprovalStatus.APPROVED;
            log.info("Manager id: {} approved leave id: {}", managerId, leaveApplicationId);

        } else if (request.getDecision() == ApprovalStatus.REJECTED) {
            leaveStateMachine.validateTransition(application.getStatus(), LeaveStatus.REJECTED);
            newStatus = LeaveStatus.REJECTED;
            approvalStatus = ApprovalStatus.REJECTED;

            // Restore balance on rejection
            leaveBalanceService.restoreBalance(
                    application.getUser().getId(),
                    application.getLeaveType().getId(),
                    application.getTotalDays()
            );
            log.info("Manager id: {} rejected leave id: {} — balance restored",
                    managerId, leaveApplicationId);

        } else {
            throw new InvalidStateTransitionException(
                "Invalid decision: " + request.getDecision()
            );
        }

        // Save approval record
        LeaveApproval approval = LeaveApproval.builder()
                .leaveApplication(application)
                .approver(manager)
                .approvalLevel(1)
                .status(approvalStatus)
                .remarks(request.getRemarks())
                .actionedAt(LocalDateTime.now())
                .build();

        LeaveApproval savedApproval = leaveApprovalRepository.save(approval);

        // Update application status
        application.setStatus(newStatus);
        leaveApplicationRepository.save(application);

        // Fire event — EmailService listens asynchronously
        eventPublisher.publishEvent(new LeaveStatusChangedEvent(
                this,
                application.getId(),
                application.getUser().getId(),
                application.getUser().getEmail(),
                application.getUser().getName(),
                previousStatus,
                newStatus,
                request.getRemarks()
        ));

        log.info("Leave application id: {} status updated to: {}", leaveApplicationId, newStatus);
        return mapToResponse(savedApproval, application);
    }

    @Override
    @Transactional
    @Auditable(action = "HR_DECISION", entityType = "LeaveApproval")
    public LeaveApprovalResponse processHRDecision(
            Long hrUserId,
            Long leaveApplicationId,
            LeaveApprovalRequest request) {

        log.info("HR user id: {} processing final decision for leave id: {}",
                hrUserId, leaveApplicationId);

        User hrUser = userRepository.findById(hrUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "HR user not found with id: " + hrUserId
                ));

        LeaveApplication application = leaveApplicationRepository
                .findById(leaveApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave application not found with id: " + leaveApplicationId
                ));

        // HR can only finalize already manager-approved leaves
        if (application.getStatus() != LeaveStatus.APPROVED) {
            throw new InvalidStateTransitionException(
                "Leave must be Manager-approved before HR can finalize. Current status: "
                    + application.getStatus()
            );
        }

        LeaveStatus previousStatus = application.getStatus();
        LeaveStatus newStatus;
        ApprovalStatus approvalStatus;

        if (request.getDecision() == ApprovalStatus.APPROVED) {
            newStatus = LeaveStatus.APPROVED;
            approvalStatus = ApprovalStatus.APPROVED;
            log.info("HR id: {} confirmed approval for leave id: {}", hrUserId, leaveApplicationId);

        } else if (request.getDecision() == ApprovalStatus.REJECTED) {
            newStatus = LeaveStatus.REJECTED;
            approvalStatus = ApprovalStatus.REJECTED;

            leaveBalanceService.restoreBalance(
                    application.getUser().getId(),
                    application.getLeaveType().getId(),
                    application.getTotalDays()
            );
            log.info("HR id: {} rejected leave id: {} — balance restored",
                    hrUserId, leaveApplicationId);

        } else {
            throw new InvalidStateTransitionException(
                "Invalid decision: " + request.getDecision()
            );
        }

        LeaveApproval approval = LeaveApproval.builder()
                .leaveApplication(application)
                .approver(hrUser)
                .approvalLevel(2)
                .status(approvalStatus)
                .remarks(request.getRemarks())
                .actionedAt(LocalDateTime.now())
                .build();

        LeaveApproval savedApproval = leaveApprovalRepository.save(approval);

        application.setStatus(newStatus);
        leaveApplicationRepository.save(application);

        eventPublisher.publishEvent(new LeaveStatusChangedEvent(
                this,
                application.getId(),
                application.getUser().getId(),
                application.getUser().getEmail(),
                application.getUser().getName(),
                previousStatus,
                newStatus,
                request.getRemarks()
        ));

        return mapToResponse(savedApproval, application);
    }

    private LeaveApprovalResponse mapToResponse(
            LeaveApproval approval, LeaveApplication application) {

        return LeaveApprovalResponse.builder()
                .id(approval.getId())
                .leaveApplicationId(application.getId())
                .approverId(approval.getApprover().getId())
                .approverName(approval.getApprover().getName())
                .approvalLevel(approval.getApprovalLevel())
                .status(approval.getStatus())
                .remarks(approval.getRemarks())
                .actionedAt(approval.getActionedAt())
                .build();
    }
}