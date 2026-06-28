package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveApplicationRequest;
import com.app.leaveManagement.dto.LeaveApplicationResponse;
import com.app.leaveManagement.entity.LeaveApplication;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveApplicationRepository;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveBalanceService;
import com.app.leaveManagement.service.LeaveStateMachine;
import com.app.leaveManagement.service.impl.LeaveApplicationServiceImpl;
import com.app.leaveManagement.util.LeaveDayCalculator;
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
class LeaveApplicationServiceImplTest {

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private LeaveStateMachine leaveStateMachine;

    @Mock
    private LeaveDayCalculator leaveDayCalculator;

    @InjectMocks
    private LeaveApplicationServiceImpl leaveApplicationService;

    private User buildUser() {
        return User.builder()
                .id(1L).name("Shankar").email("s@example.com")
                .role(Role.EMPLOYEE).isActive(true).build();
    }

    private LeaveType buildLeaveType() {
        return LeaveType.builder()
                .id(1L).name("Casual Leave")
                .maxDaysPerYear(12)
                .isActive(true).build();
    }

    @Test
    void shouldApplyLeaveSuccessfully() {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setLeaveTypeId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        User user = buildUser();
        LeaveType leaveType = buildLeaveType();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
        when(leaveApplicationRepository.findOverlappingLeaves(any(), any(), any()))
                .thenReturn(List.of());
        when(leaveDayCalculator.calculate(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(3));
        when(leaveApplicationRepository.save(any(LeaveApplication.class)))
                .thenAnswer(inv -> {
                    LeaveApplication la = inv.getArgument(0);
                    la.setId(1L);
                    return la;
                });

        LeaveApplicationResponse response = leaveApplicationService.applyLeave(1L, request);

        assertNotNull(response);
        assertEquals(LeaveStatus.PENDING, response.getStatus());
        assertEquals(BigDecimal.valueOf(3), response.getTotalDays());
        verify(leaveBalanceService).deductBalance(1L, 1L, BigDecimal.valueOf(3));
    }

    @Test
    void shouldThrowWhenOverlappingLeaveExists() {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setLeaveTypeId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser()));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(buildLeaveType()));
        when(leaveApplicationRepository.findOverlappingLeaves(any(), any(), any()))
                .thenReturn(List.of(new LeaveApplication()));

        assertThrows(InvalidStateTransitionException.class,
            () -> leaveApplicationService.applyLeave(1L, request));

        verify(leaveBalanceService, never()).deductBalance(any(), any(), any());
        verify(leaveApplicationRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenLeaveTypeNotFound() {
        LeaveApplicationRequest request = new LeaveApplicationRequest();
        request.setLeaveTypeId(99L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser()));
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> leaveApplicationService.applyLeave(1L, request));
    }

    @Test
    void shouldCancelLeaveSuccessfully() {
        User user = buildUser();
        LeaveType leaveType = buildLeaveType();

        LeaveApplication application = LeaveApplication.builder()
                .id(1L).user(user).leaveType(leaveType)
                .status(LeaveStatus.PENDING)
                .totalDays(BigDecimal.valueOf(3))
                .build();

        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(leaveApplicationRepository.save(any())).thenReturn(application);
        doNothing().when(leaveStateMachine).validateTransition(any(), any());

        LeaveApplicationResponse response = leaveApplicationService.cancelLeave(1L, 1L);

        assertEquals(LeaveStatus.CANCELLED, response.getStatus());
        verify(leaveBalanceService).restoreBalance(1L, 1L, BigDecimal.valueOf(3));
    }

    @Test
    void shouldThrowWhenCancellingAnotherUsersLeave() {
        User owner = User.builder().id(2L).build();
        LeaveApplication application = LeaveApplication.builder()
                .id(1L).user(owner)
                .status(LeaveStatus.PENDING)
                .build();

        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(InvalidStateTransitionException.class,
            () -> leaveApplicationService.cancelLeave(1L, 1L));

        verify(leaveBalanceService, never()).restoreBalance(any(), any(), any());
    }
}