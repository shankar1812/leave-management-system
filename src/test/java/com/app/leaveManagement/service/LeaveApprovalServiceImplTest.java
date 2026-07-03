package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveApprovalRequest;
import com.app.leaveManagement.dto.LeaveApprovalResponse;
import com.app.leaveManagement.entity.LeaveApproval;
import com.app.leaveManagement.entity.LeaveApplication;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.ApprovalStatus;
import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.repository.LeaveApprovalRepository;
import com.app.leaveManagement.repository.LeaveApplicationRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveBalanceService;
import com.app.leaveManagement.service.LeaveStateMachine;
import com.app.leaveManagement.service.impl.LeaveApprovalServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalServiceImplTest {

    @Mock
    private LeaveApprovalRepository leaveApprovalRepository;

    @Mock
    private LeaveApplicationRepository leaveApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private LeaveStateMachine leaveStateMachine;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LeaveApprovalServiceImpl leaveApprovalService;

    private User buildManager() {
        return User.builder()
                .id(2L).name("Manager One")
                .email("manager@example.com")
                .role(Role.MANAGER).build();
    }

    private User buildEmployee(User manager) {
        return User.builder()
                .id(1L).name("Shankar")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .manager(manager).build();
    }

    private LeaveApplication buildApplication(User employee, LeaveStatus status) {
        return LeaveApplication.builder()
                .id(1L)
                .user(employee)
                .leaveType(LeaveType.builder().id(1L).name("Casual Leave").build())
                .status(status)
                .totalDays(BigDecimal.valueOf(3))
                .build();
    }

    @Test
    void shouldApproveLeaveAsManager() {
        User manager = buildManager();
        User employee = buildEmployee(manager);
        LeaveApplication application = buildApplication(employee, LeaveStatus.PENDING);
        LeaveApprovalRequest request = new LeaveApprovalRequest(ApprovalStatus.APPROVED, "Approved");

        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(leaveApprovalRepository.findByLeaveApplicationIdAndApprovalLevel(1L, 1))
                .thenReturn(Optional.empty());
        when(leaveApprovalRepository.save(any(LeaveApproval.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(leaveApplicationRepository.save(any())).thenReturn(application);
        doNothing().when(leaveStateMachine).validateTransition(any(), any());

        LeaveApprovalResponse response =
            leaveApprovalService.processManagerDecision(2L, 1L, request);

        assertNotNull(response);
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals(1, response.getApprovalLevel());
        verify(leaveBalanceService, never()).restoreBalance(any(), any(), any());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldRestoreBalanceOnManagerRejection() {
        User manager = buildManager();
        User employee = buildEmployee(manager);
        LeaveApplication application = buildApplication(employee, LeaveStatus.PENDING);
        LeaveApprovalRequest request = new LeaveApprovalRequest(ApprovalStatus.REJECTED, "Not enough coverage");

        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(leaveApprovalRepository.findByLeaveApplicationIdAndApprovalLevel(1L, 1))
                .thenReturn(Optional.empty());
        when(leaveApprovalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(leaveApplicationRepository.save(any())).thenReturn(application);
        doNothing().when(leaveStateMachine).validateTransition(any(), any());

        leaveApprovalService.processManagerDecision(2L, 1L, request);

        verify(leaveBalanceService).restoreBalance(1L, 1L, BigDecimal.valueOf(3));
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void shouldThrowWhenManagerActionsAnotherTeamsMember() {
        User otherManager = User.builder().id(99L).build();
        User employee = buildEmployee(otherManager);
        LeaveApplication application = buildApplication(employee, LeaveStatus.PENDING);

        when(userRepository.findById(2L)).thenReturn(Optional.of(buildManager()));
        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(application));

        assertThrows(InvalidStateTransitionException.class,
            () -> leaveApprovalService.processManagerDecision(
                2L, 1L,
                new LeaveApprovalRequest(ApprovalStatus.APPROVED, null)
            )
        );

        verify(leaveApprovalRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowWhenLeaveAlreadyActioned() {
        User manager = buildManager();
        User employee = buildEmployee(manager);
        LeaveApplication application = buildApplication(employee, LeaveStatus.PENDING);

        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveApplicationRepository.findById(1L)).thenReturn(Optional.of(application));
        when(leaveApprovalRepository.findByLeaveApplicationIdAndApprovalLevel(1L, 1))
                .thenReturn(Optional.of(new LeaveApproval()));

        assertThrows(InvalidStateTransitionException.class,
            () -> leaveApprovalService.processManagerDecision(
                2L, 1L,
                new LeaveApprovalRequest(ApprovalStatus.APPROVED, null)
            )
        );
    }
}