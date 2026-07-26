package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveTypeRequest;
import com.app.leaveManagement.dto.LeaveTypeResponse;

import java.util.List;

public interface LeaveTypeService {
    LeaveTypeResponse createLeaveType(LeaveTypeRequest request);
    LeaveTypeResponse getLeaveTypeById(Long id);
    List<LeaveTypeResponse> getAllActiveLeaveTypes();
    LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request);
    void deactivateLeaveType(Long id);
}