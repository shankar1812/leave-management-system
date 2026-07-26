package com.app.leaveManagement.service;

import jakarta.servlet.http.HttpServletResponse;

public interface PdfReportService {

    void generateMonthlyAttendanceReport(
        Long userId,
        Integer month,
        Integer year,
        HttpServletResponse response
    );
}