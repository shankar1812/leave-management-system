package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.WfhStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WfhDecisionRequest {

    @NotNull(message = "Decision is required")
    private WfhStatus decision; // APPROVED or REJECTED only

    private String remarks;
}