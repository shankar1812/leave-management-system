package com.app.leaveManagement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompOffRequestDTO {

    @NotNull(message = "Worked-on date is required")
    @PastOrPresent(message = "Worked-on date must be today or in the past")
    private LocalDate workedOnDate;

    @NotNull(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    private String reason;
}