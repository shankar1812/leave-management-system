package com.app.leaveManagement.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WfhRequestDTO {

    @NotNull(message = "WFH date is required")
    @FutureOrPresent(message = "WFH date cannot be in the past")
    private LocalDate date;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}