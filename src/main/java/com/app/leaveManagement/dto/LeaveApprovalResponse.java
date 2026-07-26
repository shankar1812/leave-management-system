package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApprovalResponse {
    private Long id;
    private Long leaveApplicationId;
    private Long approverId;
    private String approverName;
    private Integer approvalLevel;
    private ApprovalStatus status;
    private String remarks;
    private LocalDateTime actionedAt;
    private LeaveApplicationResponse leaveApplication;
}