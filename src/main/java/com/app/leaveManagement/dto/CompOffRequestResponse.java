package com.app.leaveManagement.dto;

import com.app.leaveManagement.enums.CompOffStatus;
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
public class CompOffRequestResponse {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate workedOnDate;
    private String reason;
    private CompOffStatus status;
    private String approvedByName;
    private String managerRemarks;
    private LocalDateTime actionedAt;
    private LocalDateTime createdAt;
}