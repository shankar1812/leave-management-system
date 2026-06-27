package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveBalanceResponse;
import com.app.leaveManagement.entity.LeaveBalance;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.InsufficientLeaveBalanceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveBalanceRepository;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.impl.LeaveBalanceServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceImplTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LeaveBalanceServiceImpl leaveBalanceService;

    // ---------- Helper Methods ----------

    private User buildUser() {
        return User.builder()
                .id(1L)
                .name("Shankar")
                .email("s@example.com")
                .role(Role.EMPLOYEE)
                .isActive(true)
                .build();
    }

    private LeaveType buildLeaveType() {
        return LeaveType.builder()
                .id(1L)
                .name("Casual Leave")
                .maxDaysPerYear(12)
                .isCarryForwardAllowed(false)
                .isActive(true)
                .build();
    }

    // ============================================================
    // 1. deductBalance() TESTS
    // ============================================================

    @Test
    void shouldDeductBalanceSuccessfully() {
        int year = LocalDate.now().getYear();
        LeaveBalance balance = LeaveBalance.builder()
                .id(1L)
                .user(buildUser())
                .leaveType(buildLeaveType())
                .year(year)
                .totalDays(BigDecimal.valueOf(12))
                .usedDays(BigDecimal.ZERO)
                .remainingDays(BigDecimal.valueOf(12))
                .build();

        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.of(balance));

        leaveBalanceService.deductBalance(1L, 1L, BigDecimal.valueOf(3));

        assertEquals(BigDecimal.valueOf(3), balance.getUsedDays());
        assertEquals(BigDecimal.valueOf(9), balance.getRemainingDays());
        verify(leaveBalanceRepository).save(balance);
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        int year = LocalDate.now().getYear();
        LeaveBalance balance = LeaveBalance.builder()
                .id(1L)
                .user(buildUser())
                .leaveType(buildLeaveType())
                .year(year)
                .totalDays(BigDecimal.valueOf(12))
                .usedDays(BigDecimal.valueOf(11))
                .remainingDays(BigDecimal.valueOf(1))
                .build();

        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.of(balance));

        assertThrows(InsufficientLeaveBalanceException.class,
            () -> leaveBalanceService.deductBalance(1L, 1L, BigDecimal.valueOf(3)));

        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenBalanceNotFoundForDeduction() {
        int year = LocalDate.now().getYear();
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> leaveBalanceService.deductBalance(1L, 1L, BigDecimal.valueOf(3)));

        verify(leaveBalanceRepository, never()).save(any());
    }

    // ============================================================
    // 2. restoreBalance() TESTS
    // ============================================================

    @Test
    void shouldRestoreBalanceSuccessfully() {
        int year = LocalDate.now().getYear();
        LeaveBalance balance = LeaveBalance.builder()
                .id(1L)
                .user(buildUser())
                .leaveType(buildLeaveType())
                .year(year)
                .totalDays(BigDecimal.valueOf(12))
                .usedDays(BigDecimal.valueOf(5))
                .remainingDays(BigDecimal.valueOf(7))
                .build();

        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.of(balance));

        leaveBalanceService.restoreBalance(1L, 1L, BigDecimal.valueOf(3));

        assertEquals(BigDecimal.valueOf(2), balance.getUsedDays());
        assertEquals(BigDecimal.valueOf(10), balance.getRemainingDays());
        verify(leaveBalanceRepository).save(balance);
    }

    @Test
    void shouldThrowWhenBalanceNotFoundForRestore() {
        int year = LocalDate.now().getYear();
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> leaveBalanceService.restoreBalance(1L, 1L, BigDecimal.valueOf(3)));

        verify(leaveBalanceRepository, never()).save(any());
    }

    // ============================================================
    // 3. initializeBalancesForUser() TESTS
    // ============================================================

    @Test
    void shouldInitializeBalancesForNewUser() {
        User user = buildUser();
        LeaveType leaveType = buildLeaveType();
        int year = LocalDate.now().getYear();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findByIsActiveTrue()).thenReturn(List.of(leaveType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.empty());

        leaveBalanceService.initializeBalancesForUser(1L);

        verify(leaveBalanceRepository).save(any(LeaveBalance.class));
    }

    @Test
    void shouldNotDuplicateBalanceIfAlreadyExists() {
        User user = buildUser();
        LeaveType leaveType = buildLeaveType();
        int year = LocalDate.now().getYear();
        LeaveBalance existing = LeaveBalance.builder().id(1L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findByIsActiveTrue()).thenReturn(List.of(leaveType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(1L, 1L, year))
                .thenReturn(Optional.of(existing));

        leaveBalanceService.initializeBalancesForUser(1L);

        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundForInitialization() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> leaveBalanceService.initializeBalancesForUser(99L));

        verify(leaveBalanceRepository, never()).save(any());
    }

    // ============================================================
    // 4. getBalancesByUser() TESTS
    // ============================================================

    @Test
    void shouldGetBalancesForUser() {
        int year = LocalDate.now().getYear();
        User user = buildUser();
        LeaveType leaveType = buildLeaveType();

        LeaveBalance balance = LeaveBalance.builder()
                .id(1L)
                .user(user)
                .leaveType(leaveType)
                .year(year)
                .totalDays(BigDecimal.valueOf(12))
                .usedDays(BigDecimal.valueOf(3))
                .remainingDays(BigDecimal.valueOf(9))
                .build();

        when(leaveBalanceRepository.findByUserIdAndYear(1L, year))
                .thenReturn(List.of(balance));

        List<LeaveBalanceResponse> result = leaveBalanceService.getBalancesByUser(1L);

        assertEquals(1, result.size());
        assertEquals("Casual Leave", result.get(0).getLeaveTypeName());
        assertEquals(BigDecimal.valueOf(9), result.get(0).getRemainingDays());
    }

    @Test
    void shouldReturnEmptyListWhenNoBalancesForUser() {
        int year = LocalDate.now().getYear();
        when(leaveBalanceRepository.findByUserIdAndYear(1L, year))
                .thenReturn(List.of());

        List<LeaveBalanceResponse> result = leaveBalanceService.getBalancesByUser(1L);

        assertTrue(result.isEmpty());
    }
}