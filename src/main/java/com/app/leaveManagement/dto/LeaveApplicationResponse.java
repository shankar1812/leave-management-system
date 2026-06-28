package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.HalfDayType;
import com.app.leaveManagement.enums.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private HalfDayType halfDayType;
    private String reason;
    private LeaveStatus status;
    private LocalDateTime appliedAt;
}