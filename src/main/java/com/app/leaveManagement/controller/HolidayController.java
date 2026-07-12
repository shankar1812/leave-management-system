package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.HolidayRequest;
import com.app.leaveManagement.dto.HolidayResponse;
import com.app.leaveManagement.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponse> createHoliday(
            @Valid @RequestBody HolidayRequest request) {
        return new ResponseEntity<>(holidayService.createHoliday(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<HolidayResponse> getHolidayById(@PathVariable Long id) {
        return ResponseEntity.ok(holidayService.getHolidayById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<HolidayResponse>> getAllHolidays(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(holidayService.getAllHolidays(year));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponse> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequest request) {
        return ResponseEntity.ok(holidayService.updateHoliday(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}