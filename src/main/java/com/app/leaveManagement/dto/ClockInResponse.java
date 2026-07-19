package com.app.leaveManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClockInResponse {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate date;
    private LocalDateTime clockIn;
    private boolean isLate;
    private String message;
}