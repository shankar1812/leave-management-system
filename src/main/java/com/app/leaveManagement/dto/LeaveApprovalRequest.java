package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApprovalRequest {

    @NotNull(message = "Approval decision is required")
    private ApprovalStatus decision;

    private String remarks;
}