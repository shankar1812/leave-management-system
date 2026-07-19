package com.app.leaveManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {
    private Long userId;
    private String userName;
    private Integer month;
    private Integer year;
    private Long presentDays;
    private Long absentDays;
    private Long lateDays;
    private Long halfDays;
    private BigDecimal totalWorkHours;
}