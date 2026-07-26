package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.ClockInResponse;
import com.app.leaveManagement.dto.ClockOutResponse;
import com.app.leaveManagement.entity.AttendanceRecord;
import com.app.leaveManagement.entity.Department;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.AttendanceStatus;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.repository.AttendanceRepository;
import com.app.leaveManagement.repository.LeaveApplicationRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.impl.AttendanceServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeaveApplicationRepository leaveApplicationRepository;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private User buildUser() {
        Department dept = Department.builder()
                .id(1L)
                .name("Engineering")
                .shiftStartTime(LocalTime.of(9, 0))
                .build();

        return User.builder()
                .id(1L)
                .name("Shankar Sahu")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .department(dept)
                .isActive(true)
                .build();
    }

    // ---------- clockIn ----------

    @Test
    void shouldClockInSuccessfully() {
        User user = buildUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(inv -> {
                    AttendanceRecord r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });

        ClockInResponse response = attendanceService.clockIn(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertNotNull(response.getClockIn());
        verify(attendanceRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void shouldThrowWhenAlreadyClockedInToday() {
        User user = buildUser();
        AttendanceRecord existing = AttendanceRecord.builder()
                .id(1L)
                .user(user)
                .date(LocalDate.now())
                .clockIn(LocalDateTime.now().minusHours(2))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(attendanceRepository.findByUserIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(existing));

        assertThrows(InvalidStateTransitionException.class, () -> attendanceService.clockIn(1L));
        verify(attendanceRepository, never()).save(any());
    }

    // ---------- clockOut ----------

    @Test
    void shouldClockOutSuccessfully() {
        User user = buildUser();
        AttendanceRecord record = AttendanceRecord.builder()
                .id(1L)
                .user(user)
                .date(LocalDate.now())
                .clockIn(LocalDateTime.now().minusHours(8))
                .isLate(false)
                .status(AttendanceStatus.PRESENT)
                .build();

        when(attendanceRepository.findByUserIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClockOutResponse response = attendanceService.clockOut(1L);

        assertNotNull(response);
        assertNotNull(response.getClockOut());
        assertNotNull(response.getWorkHours());
        assertTrue(response.getWorkHours().compareTo(BigDecimal.ZERO) > 0);
        verify(attendanceRepository).save(any());
    }

    @Test
    void shouldThrowWhenClockingOutWithoutClockIn() {
        when(attendanceRepository.findByUserIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThrows(InvalidStateTransitionException.class, () -> attendanceService.clockOut(1L));
    }

    @Test
    void shouldThrowWhenAlreadyClockedOut() {
        User user = buildUser();
        AttendanceRecord record = AttendanceRecord.builder()
                .id(1L)
                .user(user)
                .date(LocalDate.now())
                .clockIn(LocalDateTime.now().minusHours(8))
                .clockOut(LocalDateTime.now().minusHours(1))
                .status(AttendanceStatus.PRESENT)
                .build();

        when(attendanceRepository.findByUserIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));

        assertThrows(InvalidStateTransitionException.class, () -> attendanceService.clockOut(1L));
        verify(attendanceRepository, never()).save(any());
    }

    // ---------- markAbsentForDate ----------

    @Test
    void shouldMarkAbsentForUsersWithNoAttendance() {
        User user = buildUser();
        LocalDate date = LocalDate.now();

        when(attendanceRepository.findUsersWithNoAttendanceForDate(date))
                .thenReturn(List.of(user));
        when(leaveApplicationRepository.findOverlappingLeaves(1L, date, date))
                .thenReturn(List.of());

        attendanceService.markAbsentForDate(date);

        verify(attendanceRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void shouldSkipAbsentMarkingForUsersOnApprovedLeave() {
        User user = buildUser();
        LocalDate date = LocalDate.now();

        com.app.leaveManagement.entity.LeaveApplication approvedLeave =
            com.app.leaveManagement.entity.LeaveApplication.builder()
                .id(1L)
                .status(com.app.leaveManagement.enums.LeaveStatus.APPROVED)
                .build();

        when(attendanceRepository.findUsersWithNoAttendanceForDate(date))
                .thenReturn(List.of(user));
        when(leaveApplicationRepository.findOverlappingLeaves(1L, date, date))
                .thenReturn(List.of(approvedLeave));

        attendanceService.markAbsentForDate(date);

        verify(attendanceRepository, never()).save(any());
    }
}