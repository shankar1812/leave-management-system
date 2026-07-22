package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.AttendanceResponse;
import com.app.leaveManagement.dto.ClockInResponse;
import com.app.leaveManagement.dto.ClockOutResponse;
import com.app.leaveManagement.dto.MonthlySummaryResponse;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    @PostMapping("/clock-in")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ClockInResponse> clockIn(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(attendanceService.clockIn(userId));
    }

    @PostMapping("/clock-out")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ClockOutResponse> clockOut(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(attendanceService.clockOut(userId));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or #userId == authentication.principal.id")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(
            @PathVariable Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return ResponseEntity.ok(attendanceService.getAttendanceByUserAndMonth(userId, month, year));
    }

    @GetMapping("/summary/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or #userId == authentication.principal.id")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @PathVariable Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return ResponseEntity.ok(attendanceService.getMonthlySummary(userId, month, year));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}