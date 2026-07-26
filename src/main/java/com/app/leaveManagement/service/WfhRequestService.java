package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.WfhDecisionRequest;
import com.app.leaveManagement.dto.WfhRequestDTO;
import com.app.leaveManagement.dto.WfhRequestResponse;
import com.app.leaveManagement.enums.WfhStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WfhRequestService {

    WfhRequestResponse submitWfhRequest(Long userId, WfhRequestDTO request);

    WfhRequestResponse processManagerDecision(
        Long managerId, Long wfhRequestId, WfhDecisionRequest decision
    );

    WfhRequestResponse cancelWfhRequest(Long userId, Long wfhRequestId);

    Page<WfhRequestResponse> getMyWfhRequests(Long userId, WfhStatus status, Pageable pageable);

    Page<WfhRequestResponse> getTeamWfhRequests(Long managerId, WfhStatus status, Pageable pageable);
}