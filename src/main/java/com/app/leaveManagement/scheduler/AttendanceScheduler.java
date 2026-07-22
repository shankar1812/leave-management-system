package com.app.leaveManagement.scheduler;

import com.app.leaveManagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    // Runs at 10:30 AM every Monday to Friday
    @Scheduled(cron = "0 30 10 * * MON-FRI")
    public void markAbsentEmployees() {
        LocalDate today = LocalDate.now();

        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            log.info("Skipping absent detection – today is a weekend: {}", today);
            return;
        }

        log.info("Absent detection scheduler triggered for date: {}", today);
        attendanceService.markAbsentForDate(today);
    }
}