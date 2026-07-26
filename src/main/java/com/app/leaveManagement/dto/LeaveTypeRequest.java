package com.app.leaveManagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeRequest {

    @NotBlank(message = "Leave type name is required")
    private String name;

    @NotNull(message = "Max days per year is required")
    @Min(value = 1, message = "Max days must be at least 1")
    private Integer maxDaysPerYear;

    @NotNull(message = "Carry forward flag is required")
    private Boolean isCarryForwardAllowed;
}