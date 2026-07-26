package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveApplicationRequest;
import com.app.leaveManagement.dto.LeaveApplicationResponse;
import com.app.leaveManagement.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LeaveApplicationService {

    LeaveApplicationResponse applyLeave(Long userId, LeaveApplicationRequest request);

    LeaveApplicationResponse cancelLeave(Long userId, Long leaveApplicationId);

    LeaveApplicationResponse getLeaveById(Long leaveApplicationId);

    Page<LeaveApplicationResponse> getLeavesByUser(Long userId, LeaveStatus status, Pageable pageable);

    Page<LeaveApplicationResponse> getLeavesByManager(Long managerId, LeaveStatus status, Pageable pageable);
}