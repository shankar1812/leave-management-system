package com.app.leaveManagement.util;

import com.app.leaveManagement.enums.HalfDayType;
import com.app.leaveManagement.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveDayCalculator {

    private final HolidayRepository holidayRepository;

    public BigDecimal calculate(LocalDate startDate, LocalDate endDate, HalfDayType halfDayType) {
        if (halfDayType != null) {
            log.debug("Half-day leave calculated as 0.5 days");
            return BigDecimal.valueOf(0.5);
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        List<LocalDate> holidays = holidayRepository.findHolidayDatesBetween(startDate, endDate);

        BigDecimal totalDays = BigDecimal.ZERO;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (!isWeekend(current) && !holidays.contains(current)) {
                totalDays = totalDays.add(BigDecimal.ONE);
            }
            current = current.plusDays(1);
        }

        log.debug("Calculated {} working days from {} to {}", totalDays, startDate, endDate);
        return totalDays;
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}