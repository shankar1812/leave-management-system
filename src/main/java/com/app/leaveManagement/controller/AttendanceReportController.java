package com.app.leaveManagement.controller;

import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.PdfReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance/report")
@RequiredArgsConstructor
public class AttendanceReportController {

    private final PdfReportService pdfReportService;
    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or #userId == authentication.principal.id")
    public void generateReport(
            @PathVariable Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            HttpServletResponse response) {

        pdfReportService.generateMonthlyAttendanceReport(userId, month, year, response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public void generateMyReport(
            @RequestParam Integer month,
            @RequestParam Integer year,
            HttpServletResponse response,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();

        pdfReportService.generateMonthlyAttendanceReport(userId, month, year, response);
    }
}