package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.HolidayRequest;
import com.app.leaveManagement.dto.HolidayResponse;
import com.app.leaveManagement.entity.Holiday;
import com.app.leaveManagement.enums.HolidayType;
import com.app.leaveManagement.exception.DuplicateResourceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.HolidayRepository;
import com.app.leaveManagement.service.impl.HolidayServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayServiceImplTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayServiceImpl holidayService;

    @Test
    void shouldCreateHolidaySuccessfully() {
        HolidayRequest request = new HolidayRequest(
            "Independence Day",
            LocalDate.of(2026, 8, 15),
            HolidayType.NATIONAL
        );

        when(holidayRepository.existsByDateAndName(any(), any())).thenReturn(false);
        when(holidayRepository.save(any(Holiday.class))).thenAnswer(inv -> {
            Holiday h = inv.getArgument(0);
            h.setId(1L);
            return h;
        });

        HolidayResponse response = holidayService.createHoliday(request);

        assertNotNull(response);
        assertEquals("Independence Day", response.getName());
        assertEquals(HolidayType.NATIONAL, response.getType());
    }

    @Test
    void shouldThrowWhenDuplicateHoliday() {
        HolidayRequest request = new HolidayRequest(
            "Independence Day",
            LocalDate.of(2026, 8, 15),
            HolidayType.NATIONAL
        );

        when(holidayRepository.existsByDateAndName(any(), any())).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
            () -> holidayService.createHoliday(request));
        verify(holidayRepository, never()).save(any());
    }

    @Test
    void shouldGetAllHolidaysForCurrentYear() {
        Holiday h1 = Holiday.builder()
                .id(1L).name("Republic Day")
                .date(LocalDate.of(2026, 1, 26))
                .type(HolidayType.NATIONAL).build();

        when(holidayRepository.findByYear(2026)).thenReturn(List.of(h1));

        List<HolidayResponse> result = holidayService.getAllHolidays(2026);

        assertEquals(1, result.size());
        assertEquals("Republic Day", result.get(0).getName());
    }

    @Test
    void shouldDeleteHolidaySuccessfully() {
        Holiday holiday = Holiday.builder().id(1L).name("Diwali").build();

        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday));

        holidayService.deleteHoliday(1L);

        verify(holidayRepository).delete(holiday);
    }

    @Test
    void shouldThrowWhenHolidayNotFound() {
        when(holidayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
            () -> holidayService.deleteHoliday(99L));
    }
}