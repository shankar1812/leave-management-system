package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveApprovalRequest;
import com.app.leaveManagement.dto.LeaveApprovalResponse;

public interface LeaveApprovalService {

    LeaveApprovalResponse processManagerDecision(
        Long managerId,
        Long leaveApplicationId,
        LeaveApprovalRequest request
    );

    LeaveApprovalResponse processHRDecision(
        Long hrUserId,
        Long leaveApplicationId,
        LeaveApprovalRequest request
    );
}