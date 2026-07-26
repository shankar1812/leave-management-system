package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.dto.CompOffDecisionRequest;
import com.app.leaveManagement.dto.CompOffRequestDTO;
import com.app.leaveManagement.dto.CompOffRequestResponse;
import com.app.leaveManagement.entity.CompOffRequest;
import com.app.leaveManagement.entity.LeaveBalance;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.CompOffStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.CompOffRequestRepository;
import com.app.leaveManagement.repository.LeaveBalanceRepository;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.CompOffRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompOffRequestServiceImpl implements CompOffRequestService {

    private final CompOffRequestRepository compOffRequestRepository;
    private final UserRepository userRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    private static final String COMP_OFF_LEAVE_TYPE_NAME = "Comp-off";

    @Override
    @Transactional
    @Auditable(action = "SUBMIT_COMP_OFF", entityType = "CompOffRequest")
    public CompOffRequestResponse submitCompOffRequest(Long userId, CompOffRequestDTO request) {
        log.info("User id: {} submitting comp-off request for worked date: {}",
                userId, request.getWorkedOnDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with id: " + userId
                ));

        // Employee must have a manager
        if (user.getManager() == null) {
            throw new InvalidStateTransitionException(
                "No manager assigned. Please contact HR."
            );
        }

        // Prevent duplicate comp-off for same worked date
        compOffRequestRepository
                .findByUserIdAndWorkedOnDate(userId, request.getWorkedOnDate())
                .ifPresent(existing -> {
                    throw new InvalidStateTransitionException(
                        "Comp-off request already exists for date: " +
                        request.getWorkedOnDate() +
                        " with status: " + existing.getStatus()
                    );
                });

        CompOffRequest compOffRequest = CompOffRequest.builder()
                .user(user)
                .workedOnDate(request.getWorkedOnDate())
                .reason(request.getReason())
                .status(CompOffStatus.PENDING)
                .build();

        CompOffRequest saved = compOffRequestRepository.save(compOffRequest);
        log.info("Comp-off request created with id: {} for user id: {}",
                saved.getId(), userId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "COMP_OFF_MANAGER_DECISION", entityType = "CompOffRequest")
    public CompOffRequestResponse processManagerDecision(
            Long managerId, Long compOffRequestId, CompOffDecisionRequest decision) {

        log.info("Manager id: {} processing comp-off request id: {}",
                managerId, compOffRequestId);

        if (decision.getDecision() == CompOffStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Decision must be APPROVED or REJECTED"
            );
        }

        CompOffRequest compOffRequest = compOffRequestRepository
                .findById(compOffRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Comp-off request not found with id: " + compOffRequestId
                ));

        // Only the employee's direct manager can action this
        if (compOffRequest.getUser().getManager() == null ||
                !compOffRequest.getUser().getManager().getId().equals(managerId)) {
            throw new InvalidStateTransitionException(
                "You can only action comp-off requests of your direct reports"
            );
        }

        // Only PENDING can be actioned
        if (compOffRequest.getStatus() != CompOffStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Comp-off request is already " + compOffRequest.getStatus()
            );
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Manager not found with id: " + managerId
                ));

        compOffRequest.setStatus(decision.getDecision());
        compOffRequest.setApprovedBy(manager);
        compOffRequest.setManagerRemarks(decision.getRemarks());
        compOffRequest.setActionedAt(LocalDateTime.now());

        // On approval — credit 1 comp-off day to leave balance
        if (decision.getDecision() == CompOffStatus.APPROVED) {
            creditCompOffBalance(compOffRequest.getUser());
        }

        CompOffRequest updated = compOffRequestRepository.save(compOffRequest);
        log.info("Comp-off request id: {} {} by manager id: {}",
                compOffRequestId, decision.getDecision(), managerId);

        return mapToResponse(updated);
    }

    @Override
    public Page<CompOffRequestResponse> getMyCompOffRequests(
            Long userId, CompOffStatus status, Pageable pageable) {

        if (status != null) {
            return compOffRequestRepository
                    .findByUserIdAndStatus(userId, status, pageable)
                    .map(this::mapToResponse);
        }
        return compOffRequestRepository
                .findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<CompOffRequestResponse> getTeamCompOffRequests(
            Long managerId, CompOffStatus status, Pageable pageable) {

        if (status != null) {
            return compOffRequestRepository
                    .findByUserManagerIdAndStatus(managerId, status, pageable)
                    .map(this::mapToResponse);
        }
        return compOffRequestRepository
                .findByUserManagerId(managerId, pageable)
                .map(this::mapToResponse);
    }

    // ── Private helpers ──────────────────────────────────────

    private void creditCompOffBalance(User user) {
        log.info("Crediting 1 comp-off day to user id: {}", user.getId());

        LeaveType compOffType = leaveTypeRepository.findByName(COMP_OFF_LEAVE_TYPE_NAME)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Comp-off leave type not found. Please ask Admin to create " +
                    "a leave type named '" + COMP_OFF_LEAVE_TYPE_NAME + "'"
                ));

        int currentYear = LocalDate.now().getYear();

        Optional<LeaveBalance> existingBalance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(
                    user.getId(), compOffType.getId(), currentYear
                );

        if (existingBalance.isPresent()) {
            LeaveBalance balance = existingBalance.get();
            balance.setTotalDays(balance.getTotalDays().add(BigDecimal.ONE));
            balance.setRemainingDays(balance.getRemainingDays().add(BigDecimal.ONE));
            leaveBalanceRepository.save(balance);
            log.info("Comp-off balance updated for user id: {}. New remaining: {}",
                    user.getId(), balance.getRemainingDays());
        } else {
            // First comp-off ever for this user — create balance record
            LeaveBalance newBalance = LeaveBalance.builder()
                    .user(user)
                    .leaveType(compOffType)
                    .year(currentYear)
                    .totalDays(BigDecimal.ONE)
                    .usedDays(BigDecimal.ZERO)
                    .remainingDays(BigDecimal.ONE)
                    .build();
            leaveBalanceRepository.save(newBalance);
            log.info("New comp-off balance created for user id: {}", user.getId());
        }
    }

    private CompOffRequestResponse mapToResponse(CompOffRequest request) {
        return CompOffRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .userName(request.getUser().getName())
                .workedOnDate(request.getWorkedOnDate())
                .reason(request.getReason())
                .status(request.getStatus())
                .approvedByName(request.getApprovedBy() != null
                    ? request.getApprovedBy().getName() : null)
                .managerRemarks(request.getManagerRemarks())
                .actionedAt(request.getActionedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}