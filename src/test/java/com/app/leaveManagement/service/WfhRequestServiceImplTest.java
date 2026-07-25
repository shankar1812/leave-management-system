package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.WfhDecisionRequest;
import com.app.leaveManagement.dto.WfhRequestDTO;
import com.app.leaveManagement.dto.WfhRequestResponse;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.entity.WfhRequest;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.enums.WfhStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.repository.WfhRequestRepository;
import com.app.leaveManagement.service.impl.WfhRequestServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WfhRequestServiceImplTest {

    @Mock private WfhRequestRepository wfhRequestRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WfhRequestServiceImpl wfhRequestService;

    private User buildManager() {
        return User.builder()
                .id(2L).name("Manager One")
                .email("manager@example.com")
                .role(Role.MANAGER).build();
    }

    private User buildEmployee() {
        User manager = buildManager();
        return User.builder()
                .id(1L).name("Shankar Sahu")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .manager(manager).build();
    }

    // ---------- submitWfhRequest ----------

    @Test
    void shouldSubmitWfhRequestSuccessfully() {
        User employee = buildEmployee();
        WfhRequestDTO request = new WfhRequestDTO(
            LocalDate.now().plusDays(1), "Doctor appointment in the morning"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(wfhRequestRepository.findByUserIdAndDate(1L, request.getDate()))
                .thenReturn(Optional.empty());
        when(wfhRequestRepository.save(any(WfhRequest.class))).thenAnswer(inv -> {
            WfhRequest w = inv.getArgument(0);
            w.setId(1L);
            return w;
        });

        WfhRequestResponse response = wfhRequestService.submitWfhRequest(1L, request);

        assertNotNull(response);
        assertEquals(WfhStatus.PENDING, response.getStatus());
        assertEquals(LocalDate.now().plusDays(1), response.getDate());
        verify(wfhRequestRepository).save(any(WfhRequest.class));
    }

    @Test
    void shouldThrowWhenDuplicateWfhRequestForSameDate() {
        User employee = buildEmployee();
        WfhRequestDTO request = new WfhRequestDTO(
            LocalDate.now().plusDays(1), "WFH"
        );

        WfhRequest existing = WfhRequest.builder()
                .id(1L).status(WfhStatus.PENDING).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(wfhRequestRepository.findByUserIdAndDate(1L, request.getDate()))
                .thenReturn(Optional.of(existing));

        assertThrows(InvalidStateTransitionException.class,
            () -> wfhRequestService.submitWfhRequest(1L, request));
        verify(wfhRequestRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmployeeHasNoManager() {
        User employee = User.builder()
                .id(1L).name("No Manager Employee")
                .role(Role.EMPLOYEE)
                .manager(null) // no manager assigned
                .build();

        WfhRequestDTO request = new WfhRequestDTO(LocalDate.now().plusDays(1), "WFH");

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(wfhRequestRepository.findByUserIdAndDate(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(InvalidStateTransitionException.class,
            () -> wfhRequestService.submitWfhRequest(1L, request));
    }

    // ---------- processManagerDecision ----------

    @Test
    void shouldApproveWfhRequestAsManager() {
        User manager = buildManager();
        User employee = buildEmployee();

        WfhRequest wfhRequest = WfhRequest.builder()
                .id(1L).user(employee)
                .date(LocalDate.now().plusDays(1))
                .status(WfhStatus.PENDING).build();

        WfhDecisionRequest decision = new WfhDecisionRequest(WfhStatus.APPROVED, "Approved");

        when(wfhRequestRepository.findById(1L)).thenReturn(Optional.of(wfhRequest));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(wfhRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WfhRequestResponse response = wfhRequestService.processManagerDecision(2L, 1L, decision);

        assertEquals(WfhStatus.APPROVED, response.getStatus());
        assertEquals("Approved", response.getManagerRemarks());
        verify(wfhRequestRepository).save(any());
    }

    @Test
    void shouldThrowWhenManagerActionsAnotherTeamsMember() {
        User otherManager = User.builder().id(99L).build();
        User employee = User.builder()
                .id(1L).name("Other Employee")
                .manager(otherManager).build();

        WfhRequest wfhRequest = WfhRequest.builder()
                .id(1L).user(employee)
                .status(WfhStatus.PENDING).build();

        when(wfhRequestRepository.findById(1L)).thenReturn(Optional.of(wfhRequest));

        // Manager id 2 trying to action employee whose manager is id 99
        assertThrows(InvalidStateTransitionException.class,
            () -> wfhRequestService.processManagerDecision(
                2L, 1L, new WfhDecisionRequest(WfhStatus.APPROVED, null)
            )
        );
        verify(wfhRequestRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenActioningAlreadyProcessedRequest() {
        User manager = buildManager();
        User employee = buildEmployee();

        WfhRequest wfhRequest = WfhRequest.builder()
                .id(1L).user(employee)
                .status(WfhStatus.APPROVED) // already approved
                .build();

        when(wfhRequestRepository.findById(1L)).thenReturn(Optional.of(wfhRequest));

        assertThrows(InvalidStateTransitionException.class,
            () -> wfhRequestService.processManagerDecision(
                2L, 1L, new WfhDecisionRequest(WfhStatus.REJECTED, null)
            )
        );
    }

    // ---------- cancelWfhRequest ----------

    @Test
    void shouldCancelPendingWfhRequestSuccessfully() {
        User employee = buildEmployee();
        WfhRequest wfhRequest = WfhRequest.builder()
                .id(1L).user(employee)
                .status(WfhStatus.PENDING).build();

        when(wfhRequestRepository.findById(1L)).thenReturn(Optional.of(wfhRequest));
        when(wfhRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WfhRequestResponse response = wfhRequestService.cancelWfhRequest(1L, 1L);

        assertEquals(WfhStatus.REJECTED, response.getStatus());
        assertEquals("Cancelled by employee", response.getManagerRemarks());
    }

    @Test
    void shouldThrowWhenCancellingAnotherUsersWfhRequest() {
        User otherEmployee = User.builder().id(99L).build();
        WfhRequest wfhRequest = WfhRequest.builder()
                .id(1L).user(otherEmployee)
                .status(WfhStatus.PENDING).build();

        when(wfhRequestRepository.findById(1L)).thenReturn(Optional.of(wfhRequest));

        // User 1 trying to cancel request belonging to user 99
        assertThrows(InvalidStateTransitionException.class,
            () -> wfhRequestService.cancelWfhRequest(1L, 1L));
    }

    // ---------- getMyWfhRequests ----------

    @Test
    void shouldReturnPagedWfhRequestsForUser() {
        User employee = buildEmployee();
        WfhRequest req = WfhRequest.builder()
                .id(1L).user(employee)
                .date(LocalDate.now().plusDays(2))
                .status(WfhStatus.PENDING).build();

        Page<WfhRequest> page = new PageImpl<>(List.of(req));
        when(wfhRequestRepository.findByUserId(1L, PageRequest.of(0, 10)))
                .thenReturn(page);

        Page<WfhRequestResponse> result =
            wfhRequestService.getMyWfhRequests(1L, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(WfhStatus.PENDING, result.getContent().get(0).getStatus());
    }
}