package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.dto.LeaveApplicationRequest;
import com.app.leaveManagement.dto.LeaveApplicationResponse;
import com.app.leaveManagement.entity.LeaveApplication;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveApplicationRepository;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveApplicationService;
import com.app.leaveManagement.service.LeaveBalanceService;
import com.app.leaveManagement.service.LeaveStateMachine;
import com.app.leaveManagement.util.LeaveDayCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveStateMachine leaveStateMachine;
    private final LeaveDayCalculator leaveDayCalculator;

    @Override
    @Transactional
    @Auditable(action = "APPLY_LEAVE", entityType = "LeaveApplication")
    public LeaveApplicationResponse applyLeave(Long userId, LeaveApplicationRequest request) {
        log.info("User id: {} applying for leave from {} to {}",
                userId, request.getStartDate(), request.getEndDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with id: " + request.getLeaveTypeId()));

        if (!leaveType.isActive()) {
            throw new InvalidStateTransitionException("Leave type is not active: " + leaveType.getName());
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        List<LeaveApplication> overlapping = leaveApplicationRepository.findOverlappingLeaves(
                userId, request.getStartDate(), request.getEndDate()
        );

        if (!overlapping.isEmpty()) {
            log.warn("Overlapping leave found for user id: {} on dates {} to {}",
                    userId, request.getStartDate(), request.getEndDate());
            throw new InvalidStateTransitionException("You already have a leave application overlapping these dates");
        }

        BigDecimal totalDays = leaveDayCalculator.calculate(
                request.getStartDate(),
                request.getEndDate(),
                request.getHalfDayType()
        );

        if (totalDays.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Selected date range has no working days — check for weekends and holidays");
        }

        leaveBalanceService.deductBalance(userId, leaveType.getId(), totalDays);

        LeaveApplication application = LeaveApplication.builder()
                .user(user)
                .leaveType(leaveType)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .halfDayType(request.getHalfDayType())
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveApplication saved = leaveApplicationRepository.save(application);
        log.info("Leave application created successfully with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "CANCEL_LEAVE", entityType = "LeaveApplication")
    public LeaveApplicationResponse cancelLeave(Long userId, Long leaveApplicationId) {
        log.info("User id: {} cancelling leave application id: {}", userId, leaveApplicationId);

        LeaveApplication application = leaveApplicationRepository.findById(leaveApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + leaveApplicationId));

        if (!application.getUser().getId().equals(userId)) {
            throw new InvalidStateTransitionException("You can only cancel your own leave applications");
        }

        leaveStateMachine.validateTransition(application.getStatus(), LeaveStatus.CANCELLED);

        leaveBalanceService.restoreBalance(
                userId,
                application.getLeaveType().getId(),
                application.getTotalDays()
        );

        application.setStatus(LeaveStatus.CANCELLED);
        LeaveApplication updated = leaveApplicationRepository.save(application);

        log.info("Leave application id: {} cancelled successfully", leaveApplicationId);
        return mapToResponse(updated);
    }

    @Override
    public LeaveApplicationResponse getLeaveById(Long leaveApplicationId) {
        LeaveApplication application = leaveApplicationRepository.findById(leaveApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found with id: " + leaveApplicationId));
        return mapToResponse(application);
    }

    @Override
    public Page<LeaveApplicationResponse> getLeavesByUser(Long userId, LeaveStatus status, Pageable pageable) {
        log.info("Fetching leaves for user id: {} with status: {}", userId, status);

        if (status != null) {
            return leaveApplicationRepository.findByUserIdAndStatus(userId, status, pageable)
                    .map(this::mapToResponse);
        }

        return leaveApplicationRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<LeaveApplicationResponse> getLeavesByManager(Long managerId, LeaveStatus status, Pageable pageable) {
        log.info("Fetching leaves for manager id: {} with status: {}", managerId, status);

        if (status != null) {
            return leaveApplicationRepository.findByUserManagerIdAndStatus(managerId, status, pageable)
                    .map(this::mapToResponse);
        }

        return leaveApplicationRepository.findByUserManagerId(managerId, pageable)
                .map(this::mapToResponse);
    }

    private LeaveApplicationResponse mapToResponse(LeaveApplication application) {
        return LeaveApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUser().getId())
                .userName(application.getUser().getName())
                .leaveTypeId(application.getLeaveType().getId())
                .leaveTypeName(application.getLeaveType().getName())
                .startDate(application.getStartDate())
                .endDate(application.getEndDate())
                .totalDays(application.getTotalDays())
                .halfDayType(application.getHalfDayType())
                .reason(application.getReason())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .build();
    }
}