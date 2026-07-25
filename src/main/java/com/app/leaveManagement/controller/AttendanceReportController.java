package com.app.leaveManagement.controller;

import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.PdfReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/attendance/report")
@RequiredArgsConstructor
@Tag(name = "Attendance Reports", description = "PDF report generation for monthly attendance")
public class AttendanceReportController {

    private final PdfReportService pdfReportService;
    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or #userId == authentication.principal.id")
    @Operation(
        summary = "Download monthly attendance PDF for a specific user",
        description = "Streams a PDF file directly. Use 'Save Response' in Postman to download."
    )
    public void generateReport(
            @PathVariable Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @Parameter(hidden = true) HttpServletResponse response) {

        pdfReportService.generateMonthlyAttendanceReport(userId, month, year, response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Download my monthly attendance PDF",
        description = "Streams a PDF file for the currently authenticated employee."
    )
    public void generateMyReport(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @Parameter(hidden = true) HttpServletResponse response,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();

        pdfReportService.generateMonthlyAttendanceReport(userId, month, year, response);
    }
}