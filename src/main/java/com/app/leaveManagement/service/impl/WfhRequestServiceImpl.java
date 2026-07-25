package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.dto.WfhDecisionRequest;
import com.app.leaveManagement.dto.WfhRequestDTO;
import com.app.leaveManagement.dto.WfhRequestResponse;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.entity.WfhRequest;
import com.app.leaveManagement.enums.WfhStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.repository.WfhRequestRepository;
import com.app.leaveManagement.service.WfhRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WfhRequestServiceImpl implements WfhRequestService {

    private final WfhRequestRepository wfhRequestRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Auditable(action = "SUBMIT_WFH", entityType = "WfhRequest")
    public WfhRequestResponse submitWfhRequest(Long userId, WfhRequestDTO request) {
        log.info("User id: {} submitting WFH request for date: {}", userId, request.getDate());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with id: " + userId
                ));

        // Prevent duplicate WFH request for same date
        wfhRequestRepository.findByUserIdAndDate(userId, request.getDate())
                .ifPresent(existing -> {
                    throw new InvalidStateTransitionException(
                        "WFH request already exists for date: " + request.getDate() +
                        " with status: " + existing.getStatus()
                    );
                });

        // Employee must have a manager assigned
        if (user.getManager() == null) {
            throw new InvalidStateTransitionException(
                "No manager assigned. Please contact HR to assign a manager."
            );
        }

        WfhRequest wfhRequest = WfhRequest.builder()
                .user(user)
                .date(request.getDate())
                .reason(request.getReason())
                .status(WfhStatus.PENDING)
                .build();

        WfhRequest saved = wfhRequestRepository.save(wfhRequest);
        log.info("WFH request created with id: {} for user id: {}", saved.getId(), userId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "WFH_MANAGER_DECISION", entityType = "WfhRequest")
    public WfhRequestResponse processManagerDecision(
            Long managerId, Long wfhRequestId, WfhDecisionRequest decision) {

        log.info("Manager id: {} processing WFH request id: {}", managerId, wfhRequestId);

        // Validate decision is only APPROVED or REJECTED
        if (decision.getDecision() == WfhStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Decision must be APPROVED or REJECTED, not PENDING"
            );
        }

        WfhRequest wfhRequest = wfhRequestRepository.findById(wfhRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "WFH request not found with id: " + wfhRequestId
                ));

        // Only the employee's direct manager can action this
        if (wfhRequest.getUser().getManager() == null ||
                !wfhRequest.getUser().getManager().getId().equals(managerId)) {
            log.warn("Manager id: {} attempted to action WFH not belonging to their team",
                    managerId);
            throw new InvalidStateTransitionException(
                "You can only action WFH requests of your direct reports"
            );
        }

        // Only PENDING requests can be actioned
        if (wfhRequest.getStatus() != WfhStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "WFH request is already " + wfhRequest.getStatus() +
                ". Only PENDING requests can be actioned."
            );
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Manager not found with id: " + managerId
                ));

        wfhRequest.setStatus(decision.getDecision());
        wfhRequest.setManager(manager);
        wfhRequest.setManagerRemarks(decision.getRemarks());
        wfhRequest.setActionedAt(LocalDateTime.now());

        WfhRequest updated = wfhRequestRepository.save(wfhRequest);

        log.info("WFH request id: {} {} by manager id: {}",
                wfhRequestId, decision.getDecision(), managerId);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @Auditable(action = "CANCEL_WFH", entityType = "WfhRequest")
    public WfhRequestResponse cancelWfhRequest(Long userId, Long wfhRequestId) {
        log.info("User id: {} cancelling WFH request id: {}", userId, wfhRequestId);

        WfhRequest wfhRequest = wfhRequestRepository.findById(wfhRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "WFH request not found with id: " + wfhRequestId
                ));

        // Only owner can cancel
        if (!wfhRequest.getUser().getId().equals(userId)) {
            throw new InvalidStateTransitionException(
                "You can only cancel your own WFH requests"
            );
        }

        // Only PENDING requests can be cancelled
        if (wfhRequest.getStatus() != WfhStatus.PENDING) {
            throw new InvalidStateTransitionException(
                "Only PENDING WFH requests can be cancelled. Current status: "
                + wfhRequest.getStatus()
            );
        }

        wfhRequest.setStatus(WfhStatus.REJECTED);
        wfhRequest.setManagerRemarks("Cancelled by employee");
        wfhRequest.setActionedAt(LocalDateTime.now());

        WfhRequest updated = wfhRequestRepository.save(wfhRequest);
        log.info("WFH request id: {} cancelled by user id: {}", wfhRequestId, userId);

        return mapToResponse(updated);
    }

    @Override
    public Page<WfhRequestResponse> getMyWfhRequests(
            Long userId, WfhStatus status, Pageable pageable) {

        log.info("Fetching WFH requests for user id: {} status: {}", userId, status);

        if (status != null) {
            return wfhRequestRepository
                    .findByUserIdAndStatus(userId, status, pageable)
                    .map(this::mapToResponse);
        }
        return wfhRequestRepository
                .findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<WfhRequestResponse> getTeamWfhRequests(
            Long managerId, WfhStatus status, Pageable pageable) {

        log.info("Fetching WFH requests for manager id: {} status: {}", managerId, status);

        if (status != null) {
            return wfhRequestRepository
                    .findByUserManagerIdAndStatus(managerId, status, pageable)
                    .map(this::mapToResponse);
        }
        return wfhRequestRepository
                .findByUserManagerId(managerId, pageable)
                .map(this::mapToResponse);
    }

    private WfhRequestResponse mapToResponse(WfhRequest wfhRequest) {
        return WfhRequestResponse.builder()
                .id(wfhRequest.getId())
                .userId(wfhRequest.getUser().getId())
                .userName(wfhRequest.getUser().getName())
                .date(wfhRequest.getDate())
                .reason(wfhRequest.getReason())
                .status(wfhRequest.getStatus())
                .managerRemarks(wfhRequest.getManagerRemarks())
                .actionedAt(wfhRequest.getActionedAt())
                .createdAt(wfhRequest.getCreatedAt())
                .build();
    }
}