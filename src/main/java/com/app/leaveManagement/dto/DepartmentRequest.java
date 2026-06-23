package com.app.leaveManagement.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentRequest {

    @NotBlank(message = "Department name is required")
    private String name;

    @NotNull(message = "Shift start time is required")
    private LocalTime shiftStartTime;
}
