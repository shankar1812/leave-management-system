package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.AttendanceResponse;
import com.app.leaveManagement.dto.ClockInResponse;
import com.app.leaveManagement.dto.ClockOutResponse;
import com.app.leaveManagement.dto.MonthlySummaryResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    ClockInResponse clockIn(Long userId);

    ClockOutResponse clockOut(Long userId);

    List<AttendanceResponse> getAttendanceByUserAndMonth(
        Long userId, Integer month, Integer year
    );

    MonthlySummaryResponse getMonthlySummary(Long userId, Integer month, Integer year);

    void markAbsentForDate(LocalDate date);
}