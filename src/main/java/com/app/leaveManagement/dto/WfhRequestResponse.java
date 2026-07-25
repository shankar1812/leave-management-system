package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.WfhStatus;
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
public class WfhRequestResponse {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate date;
    private String reason;
    private WfhStatus status;
    private String managerRemarks;
    private LocalDateTime actionedAt;
    private LocalDateTime createdAt;
}