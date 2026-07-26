package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveTypeRequest;
import com.app.leaveManagement.dto.LeaveTypeResponse;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.exception.DuplicateResourceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.service.impl.LeaveTypeServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveTypeServiceImplTest {

    @Mock
    private LeaveTypeRepository leaveTypeRepository;

    @InjectMocks
    private LeaveTypeServiceImpl leaveTypeService;

    @Test
    void shouldCreateLeaveTypeSuccessfully() {
        LeaveTypeRequest request = new LeaveTypeRequest("Casual Leave", 12, true);

        when(leaveTypeRepository.existsByName("Casual Leave")).thenReturn(false);
        when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(inv -> {
            LeaveType lt = inv.getArgument(0);
            lt.setId(1L);
            return lt;
        });

        LeaveTypeResponse response = leaveTypeService.createLeaveType(request);

        assertNotNull(response);
        assertEquals("Casual Leave", response.getName());
        assertEquals(12, response.getMaxDaysPerYear());
        assertTrue(response.getIsCarryForwardAllowed());
        assertTrue(response.getIsActive());
        verify(leaveTypeRepository).save(any(LeaveType.class));
    }

    @Test
    void shouldThrowWhenLeaveTypeNameAlreadyExists() {
        LeaveTypeRequest request = new LeaveTypeRequest("Casual Leave", 12, true);

        when(leaveTypeRepository.existsByName("Casual Leave")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> leaveTypeService.createLeaveType(request));
        verify(leaveTypeRepository, never()).save(any());
    }

    @Test
    void shouldGetAllActiveLeaveTypes() {
        LeaveType lt1 = LeaveType.builder().id(1L).name("Casual Leave").isActive(true).build();
        LeaveType lt2 = LeaveType.builder().id(2L).name("Sick Leave").isActive(true).build();

        when(leaveTypeRepository.findByIsActiveTrue()).thenReturn(List.of(lt1, lt2));

        List<LeaveTypeResponse> result = leaveTypeService.getAllActiveLeaveTypes();

        assertEquals(2, result.size());
        assertEquals("Casual Leave", result.get(0).getName());
    }

    @Test
    void shouldDeactivateLeaveTypeSuccessfully() {
        LeaveType leaveType = LeaveType.builder()
                .id(1L).name("Casual Leave").isActive(true).build();

        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));

        leaveTypeService.deactivateLeaveType(1L);

        assertFalse(leaveType.isActive());
        verify(leaveTypeRepository).save(leaveType);
    }

    @Test
    void shouldThrowWhenDeactivatingNonExistentLeaveType() {
        when(leaveTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> leaveTypeService.deactivateLeaveType(99L));
    }
}
