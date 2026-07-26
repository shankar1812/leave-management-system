package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.CompOffDecisionRequest;
import com.app.leaveManagement.dto.CompOffRequestDTO;
import com.app.leaveManagement.dto.CompOffRequestResponse;
import com.app.leaveManagement.entity.*;
import com.app.leaveManagement.enums.CompOffStatus;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.repository.*;
import com.app.leaveManagement.service.impl.CompOffRequestServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompOffRequestServiceImplTest {

    @Mock private CompOffRequestRepository compOffRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;

    @InjectMocks private CompOffRequestServiceImpl compOffRequestService;

    private User buildManager() {
        return User.builder()
                .id(2L).name("Manager One")
                .email("manager@example.com")
                .role(Role.MANAGER).build();
    }

    private User buildEmployee() {
        return User.builder()
                .id(1L).name("Shankar Sahu")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .manager(buildManager()).build();
    }

    private LeaveType buildCompOffLeaveType() {
        return LeaveType.builder()
                .id(10L).name("Comp-off")
                .maxDaysPerYear(0)
                .isActive(true).build();
    }

    // ---------- submitCompOffRequest ----------

    @Test
    void shouldSubmitCompOffRequestSuccessfully() {
        User employee = buildEmployee();
        CompOffRequestDTO request = new CompOffRequestDTO(
            LocalDate.now().minusDays(1), "Worked on Republic Day for production release"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(compOffRequestRepository.findByUserIdAndWorkedOnDate(1L, request.getWorkedOnDate()))
                .thenReturn(Optional.empty());
        when(compOffRequestRepository.save(any(CompOffRequest.class))).thenAnswer(inv -> {
            CompOffRequest r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        CompOffRequestResponse response =
            compOffRequestService.submitCompOffRequest(1L, request);

        assertNotNull(response);
        assertEquals(CompOffStatus.PENDING, response.getStatus());
        verify(compOffRequestRepository).save(any(CompOffRequest.class));
    }

    @Test
    void shouldThrowWhenDuplicateCompOffForSameDate() {
        User employee = buildEmployee();
        CompOffRequestDTO request = new CompOffRequestDTO(
            LocalDate.now().minusDays(1), "Worked on holiday"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(compOffRequestRepository.findByUserIdAndWorkedOnDate(any(), any()))
                .thenReturn(Optional.of(new CompOffRequest()));

        assertThrows(InvalidStateTransitionException.class,
            () -> compOffRequestService.submitCompOffRequest(1L, request));
        verify(compOffRequestRepository, never()).save(any());
    }

    // ---------- processManagerDecision — APPROVE ----------

    @Test
    void shouldApproveAndCreditBalanceWhenNoExistingBalance() {
        User manager = buildManager();
        User employee = buildEmployee();
        LeaveType compOffType = buildCompOffLeaveType();

        CompOffRequest request = CompOffRequest.builder()
                .id(1L).user(employee)
                .workedOnDate(LocalDate.now().minusDays(1))
                .status(CompOffStatus.PENDING).build();

        CompOffDecisionRequest decision =
            new CompOffDecisionRequest(CompOffStatus.APPROVED, "Well done");

        when(compOffRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveTypeRepository.findByName("Comp-off"))
                .thenReturn(Optional.of(compOffType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(
                eq(1L), eq(10L), anyInt()))
                .thenReturn(Optional.empty());
        when(compOffRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompOffRequestResponse response =
            compOffRequestService.processManagerDecision(2L, 1L, decision);

        assertEquals(CompOffStatus.APPROVED, response.getStatus());
        // New balance record created with 1 day
        verify(leaveBalanceRepository).save(argThat(balance ->
            balance.getTotalDays().compareTo(BigDecimal.ONE) == 0 &&
            balance.getRemainingDays().compareTo(BigDecimal.ONE) == 0
        ));
    }

    @Test
    void shouldApproveAndIncrementExistingBalance() {
        User manager = buildManager();
        User employee = buildEmployee();
        LeaveType compOffType = buildCompOffLeaveType();

        CompOffRequest request = CompOffRequest.builder()
                .id(1L).user(employee)
                .workedOnDate(LocalDate.now().minusDays(2))
                .status(CompOffStatus.PENDING).build();

        LeaveBalance existingBalance = LeaveBalance.builder()
                .id(5L).user(employee).leaveType(compOffType)
                .totalDays(BigDecimal.valueOf(2))
                .usedDays(BigDecimal.ONE)
                .remainingDays(BigDecimal.ONE).build();

        CompOffDecisionRequest decision =
            new CompOffDecisionRequest(CompOffStatus.APPROVED, "Approved");

        when(compOffRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leaveTypeRepository.findByName("Comp-off"))
                .thenReturn(Optional.of(compOffType));
        when(leaveBalanceRepository.findByUserIdAndLeaveTypeIdAndYear(
                eq(1L), eq(10L), anyInt()))
                .thenReturn(Optional.of(existingBalance));
        when(compOffRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        compOffRequestService.processManagerDecision(2L, 1L, decision);

        // Existing balance incremented by 1
        assertEquals(BigDecimal.valueOf(3), existingBalance.getTotalDays());
        assertEquals(BigDecimal.valueOf(2), existingBalance.getRemainingDays());
        verify(leaveBalanceRepository).save(existingBalance);
    }

    @Test
    void shouldRejectWithoutCreditingBalance() {
        User manager = buildManager();
        User employee = buildEmployee();

        CompOffRequest request = CompOffRequest.builder()
                .id(1L).user(employee)
                .status(CompOffStatus.PENDING).build();

        CompOffDecisionRequest decision =
            new CompOffDecisionRequest(CompOffStatus.REJECTED, "Not verified");

        when(compOffRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(compOffRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompOffRequestResponse response =
            compOffRequestService.processManagerDecision(2L, 1L, decision);

        assertEquals(CompOffStatus.REJECTED, response.getStatus());
        // Balance must NEVER be touched on rejection
        verify(leaveTypeRepository, never()).findByName(any());
        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenManagerActionsAnotherTeamsMember() {
        User otherManager = User.builder().id(99L).build();
        User employee = User.builder()
                .id(1L).manager(otherManager).build();

        CompOffRequest request = CompOffRequest.builder()
                .id(1L).user(employee)
                .status(CompOffStatus.PENDING).build();

        when(compOffRequestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(InvalidStateTransitionException.class,
            () -> compOffRequestService.processManagerDecision(
                2L, 1L,
                new CompOffDecisionRequest(CompOffStatus.APPROVED, null)
            )
        );
        verify(leaveBalanceRepository, never()).save(any());
    }
}