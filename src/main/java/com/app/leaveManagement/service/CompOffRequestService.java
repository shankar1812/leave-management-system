package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.CompOffDecisionRequest;
import com.app.leaveManagement.dto.CompOffRequestDTO;
import com.app.leaveManagement.dto.CompOffRequestResponse;
import com.app.leaveManagement.enums.CompOffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompOffRequestService {

    CompOffRequestResponse submitCompOffRequest(Long userId, CompOffRequestDTO request);

    CompOffRequestResponse processManagerDecision(
        Long managerId, Long compOffRequestId, CompOffDecisionRequest decision
    );

    Page<CompOffRequestResponse> getMyCompOffRequests(
        Long userId, CompOffStatus status, Pageable pageable
    );

    Page<CompOffRequestResponse> getTeamCompOffRequests(
        Long managerId, CompOffStatus status, Pageable pageable
    );
}