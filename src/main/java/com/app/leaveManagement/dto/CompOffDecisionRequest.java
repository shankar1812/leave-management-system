package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.CompOffStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompOffDecisionRequest {

    @NotNull(message = "Decision is required")
    private CompOffStatus decision;

    private String remarks;
}