package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.dto.AttendanceResponse;
import com.app.leaveManagement.dto.ClockInResponse;
import com.app.leaveManagement.dto.ClockOutResponse;
import com.app.leaveManagement.dto.MonthlySummaryResponse;
import com.app.leaveManagement.entity.AttendanceRecord;
import com.app.leaveManagement.entity.Department;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.AttendanceStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.AttendanceRepository;
import com.app.leaveManagement.repository.LeaveApplicationRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final LeaveApplicationRepository leaveApplicationRepository;

    private static final int LATE_GRACE_MINUTES = 15;

    @Override
    @Transactional
    @Auditable(action = "CLOCK_IN", entityType = "AttendanceRecord")
    public ClockInResponse clockIn(Long userId) {
        log.info("Clock-in request for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        attendanceRepository.findByUserIdAndDate(userId, today).ifPresent(existing -> {
            throw new InvalidStateTransitionException("Already clocked in today at: " + existing.getClockIn());
        });

        boolean isLate = false;
        String lateMessage = "";

        Department department = user.getDepartment();
        if (department != null && department.getShiftStartTime() != null) {
            LocalTime shiftStart = department.getShiftStartTime();
            LocalTime lateThreshold = shiftStart.plusMinutes(LATE_GRACE_MINUTES);

            if (now.toLocalTime().isAfter(lateThreshold)) {
                isLate = true;
                lateMessage = " (Late arrival — shift started at " + shiftStart + ")";
                log.info("User id: {} marked as late. Shift: {}, Clock-in: {}", userId, shiftStart, now.toLocalTime());
            }
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .user(user)
                .date(today)
                .clockIn(now)
                .isLate(isLate)
                .status(AttendanceStatus.PRESENT)
                .build();

        AttendanceRecord saved = attendanceRepository.save(record);
        log.info("Clock-in saved for user id: {} at: {}", userId, now);

        return ClockInResponse.builder()
                .id(saved.getId())
                .userId(user.getId())
                .userName(user.getName())
                .date(today)
                .clockIn(now)
                .isLate(isLate)
                .message("Clocked in successfully at " + now.toLocalTime() + lateMessage)
                .build();
    }

    @Override
    @Transactional
    @Auditable(action = "CLOCK_OUT", entityType = "AttendanceRecord")
    public ClockOutResponse clockOut(Long userId) {
        log.info("Clock-out request for user id: {}", userId);

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        AttendanceRecord record = attendanceRepository
                .findByUserIdAndDate(userId, today)
                .orElseThrow(() -> new InvalidStateTransitionException("No clock-in found for today. Please clock in first."));

        if (record.getClockOut() != null) {
            throw new InvalidStateTransitionException("Already clocked out today at: " + record.getClockOut());
        }

        long minutesWorked = ChronoUnit.MINUTES.between(record.getClockIn(), now);
        BigDecimal workHours = BigDecimal.valueOf(minutesWorked)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        record.setClockOut(now);
        record.setWorkHours(workHours);

        AttendanceRecord updated = attendanceRepository.save(record);

        User user = updated.getUser();
        return ClockOutResponse.builder()
                .id(updated.getId())
                .userId(user.getId())
                .userName(user.getName())
                .date(today)
                .clockIn(updated.getClockIn())
                .clockOut(now)
                .workHours(workHours)
                .isLate(updated.isLate())
                .build();
    }

    @Override
    public List<AttendanceResponse> getAttendanceByUserAndMonth(Long userId, Integer month, Integer year) {
        log.info("Fetching attendance for user id: {} month: {} year: {}", userId, month, year);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return attendanceRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MonthlySummaryResponse getMonthlySummary(Long userId, Integer month, Integer year) {
        log.info("Generating monthly summary for user id: {} month: {} year: {}", userId, month, year);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        Long presentDays = attendanceRepository.countByUserIdAndDateBetweenAndStatus(
                userId, startDate, endDate, AttendanceStatus.PRESENT);
        Long absentDays = attendanceRepository.countByUserIdAndDateBetweenAndStatus(
                userId, startDate, endDate, AttendanceStatus.ABSENT);
        Long halfDays = attendanceRepository.countByUserIdAndDateBetweenAndStatus(
                userId, startDate, endDate, AttendanceStatus.HALF_DAY);

        List<AttendanceRecord> records = attendanceRepository
                .findByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate);

        BigDecimal totalWorkHours = records.stream()
                .filter(r -> r.getWorkHours() != null)
                .map(AttendanceRecord::getWorkHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long lateDays = records.stream()
                .filter(AttendanceRecord::isLate)
                .count();

        return MonthlySummaryResponse.builder()
                .userId(userId)
                .userName(user.getName())
                .month(month)
                .year(year)
                .presentDays(presentDays)
                .absentDays(absentDays)
                .lateDays(lateDays)
                .halfDays(halfDays)
                .totalWorkHours(totalWorkHours)
                .build();
    }

    @Override
    @Transactional
    public void markAbsentForDate(LocalDate date) {
        log.info("Running absent detection for date: {}", date);

        List<User> usersWithNoAttendance = attendanceRepository.findUsersWithNoAttendanceForDate(date);

        for (User user : usersWithNoAttendance) {
            boolean onApprovedLeave = leaveApplicationRepository
                    .findOverlappingLeaves(user.getId(), date, date)
                    .stream()
                    .anyMatch(la -> la.getStatus().name().equals("APPROVED"));

            if (!onApprovedLeave) {
                AttendanceRecord absentRecord = AttendanceRecord.builder()
                        .user(user)
                        .date(date)
                        .isLate(false)
                        .status(AttendanceStatus.ABSENT)
                        .build();

                attendanceRepository.save(absentRecord);
                log.info("Marked ABSENT: user id: {} for date: {}", user.getId(), date);
            } else {
                log.debug("User id: {} is on approved leave for date: {} — skipping", user.getId(), date);
            }
        }

        log.info("Absent detection completed for date: {}. Processed: {} users", date, usersWithNoAttendance.size());
    }

    private AttendanceResponse mapToResponse(AttendanceRecord record) {
        return AttendanceResponse.builder()
                .id(record.getId())
                .userId(record.getUser().getId())
                .userName(record.getUser().getName())
                .date(record.getDate())
                .clockIn(record.getClockIn())
                .clockOut(record.getClockOut())
                .workHours(record.getWorkHours())
                .isLate(record.isLate())
                .status(record.getStatus())
                .build();
    }
}