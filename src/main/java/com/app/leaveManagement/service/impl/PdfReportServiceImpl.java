package com.app.leaveManagement.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.app.leaveManagement.dto.AttendanceResponse;
import com.app.leaveManagement.dto.MonthlySummaryResponse;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.AttendanceService;
import com.app.leaveManagement.service.PdfReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfReportServiceImpl implements PdfReportService {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;

    // Brand colors
    private static final DeviceRgb HEADER_BG    = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb ROW_ALT_BG   = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb SUMMARY_BG   = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb ABSENT_COLOR = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb LATE_COLOR   = new DeviceRgb(234, 88, 12);
    private static final DeviceRgb PRESENT_COLOR = new DeviceRgb(22, 163, 74);

    @Override
    public void generateMonthlyAttendanceReport(
            Long userId, Integer month, Integer year,
            HttpServletResponse response) {

        log.info("Generating PDF attendance report for user id: {} month: {} year: {}",
                userId, month, year);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with id: " + userId
                ));

        List<AttendanceResponse> records =
                attendanceService.getAttendanceByUserAndMonth(userId, month, year);

        MonthlySummaryResponse summary =
                attendanceService.getMonthlySummary(userId, month, year);

        // Set HTTP response headers for file download
        String fileName = String.format("attendance_%s_%s_%d.pdf",
                user.getName().replace(" ", "_"), Month.of(month).name(), year);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"");

        try {
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            PdfFont boldFont    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ---- HEADER ----
            Paragraph title = new Paragraph("Monthly Attendance Report")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph(
                    Month.of(month).name() + " " + year +
                    " — Leave Management System")
                    .setFont(regularFont)
                    .setFontSize(11)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(subtitle);

            // ---- EMPLOYEE INFO ----
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(20);

            addInfoRow(infoTable, "Employee Name", user.getName(), boldFont, regularFont);
            addInfoRow(infoTable, "Email", user.getEmail(), boldFont, regularFont);
            addInfoRow(infoTable, "Department",
                user.getDepartment() != null ? user.getDepartment().getName() : "N/A",
                boldFont, regularFont);
            addInfoRow(infoTable, "Report Period",
                Month.of(month).name() + " " + year, boldFont, regularFont);

            document.add(infoTable);

            // ---- SUMMARY BOX ----
            Paragraph summaryTitle = new Paragraph("Summary")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setMarginBottom(8);
            document.add(summaryTitle);

            Table summaryTable = new Table(
                    UnitValue.createPercentArray(new float[]{1, 1, 1, 1, 1}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(24);

            String[] summaryHeaders = {"Present", "Absent", "Late", "Half Day", "Total Hours"};
            for (String h : summaryHeaders) {
                summaryTable.addHeaderCell(
                    new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10))
                        .setBackgroundColor(SUMMARY_BG)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(8)
                );
            }

            summaryTable.addCell(summaryCell(
                String.valueOf(summary.getPresentDays()), PRESENT_COLOR, boldFont));
            summaryTable.addCell(summaryCell(
                String.valueOf(summary.getAbsentDays()), ABSENT_COLOR, boldFont));
            summaryTable.addCell(summaryCell(
                String.valueOf(summary.getLateDays()), LATE_COLOR, boldFont));
            summaryTable.addCell(summaryCell(
                String.valueOf(summary.getHalfDays()), ColorConstants.DARK_GRAY, boldFont));
            summaryTable.addCell(summaryCell(
                summary.getTotalWorkHours() + " hrs", ColorConstants.DARK_GRAY, boldFont));

            document.add(summaryTable);

            // ---- DAILY RECORDS TABLE ----
            Paragraph detailsTitle = new Paragraph("Daily Records")
                    .setFont(boldFont)
                    .setFontSize(13)
                    .setMarginBottom(8);
            document.add(detailsTitle);

            Table detailsTable = new Table(
                    UnitValue.createPercentArray(new float[]{0.8f, 1f, 1f, 0.8f, 0.8f, 0.8f}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Table headers
            String[] headers = {"Date", "Clock In", "Clock Out", "Hours", "Status", "Late"};
            for (String h : headers) {
                detailsTable.addHeaderCell(
                    new Cell()
                        .add(new Paragraph(h).setFont(boldFont).setFontSize(9)
                            .setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(HEADER_BG)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(6)
                );
            }

            // Table rows
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

            for (int i = 0; i < records.size(); i++) {
                AttendanceResponse record = records.get(i);
                boolean isAltRow = i % 2 == 1;
                DeviceRgb rowBg = isAltRow ? ROW_ALT_BG : null;

                // Status color
                DeviceRgb statusColor = switch (record.getStatus()) {
                    case PRESENT   -> PRESENT_COLOR;
                    case ABSENT    -> ABSENT_COLOR;
                    case HALF_DAY  -> LATE_COLOR;
                    default        -> new DeviceRgb(100, 100, 100);
                };

                detailsTable.addCell(dataCell(
                    record.getDate().format(dateFormatter), rowBg, regularFont, 9));
                detailsTable.addCell(dataCell(
                    record.getClockIn() != null
                        ? record.getClockIn().format(timeFormatter) : "—",
                    rowBg, regularFont, 9));
                detailsTable.addCell(dataCell(
                    record.getClockOut() != null
                        ? record.getClockOut().format(timeFormatter) : "—",
                    rowBg, regularFont, 9));
                detailsTable.addCell(dataCell(
                    record.getWorkHours() != null
                        ? record.getWorkHours().toString() : "—",
                    rowBg, regularFont, 9));

                // Status cell with color
                detailsTable.addCell(
                    new Cell()
                        .add(new Paragraph(record.getStatus().name())
                            .setFont(boldFont).setFontSize(9)
                            .setFontColor(statusColor))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setBackgroundColor(rowBg)
                        .setPadding(5)
                );

                detailsTable.addCell(dataCell(
                    record.isLate() ? "Yes" : "No",
                    rowBg, regularFont, 9));
            }

            document.add(detailsTable);

            // ---- FOOTER ----
            Paragraph footer = new Paragraph(
                    "Generated by Leave Management System on " +
                    java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                    .setFont(regularFont)
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(footer);

            document.close();
            log.info("PDF report generated successfully for user id: {}", userId);

        } catch (IOException e) {
            log.error("Failed to generate PDF report for user id: {} — {}", userId, e.getMessage());
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage());
        }
    }

    // ---- Private helpers ----

    private void addInfoRow(Table table, String label, String value,
                            PdfFont boldFont, PdfFont regularFont) {
        table.addCell(
            new Cell().add(new Paragraph(label).setFont(boldFont).setFontSize(10))
                .setBackgroundColor(SUMMARY_BG).setPadding(6).setBorder(null)
        );
        table.addCell(
            new Cell().add(new Paragraph(value).setFont(regularFont).setFontSize(10))
                .setPadding(6).setBorder(null)
        );
    }

    private Cell summaryCell(String value, com.itextpdf.kernel.colors.Color color,
                             PdfFont boldFont) {
        return new Cell()
                .add(new Paragraph(value).setFont(boldFont).setFontSize(14)
                    .setFontColor(color))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(10);
    }

    private Cell dataCell(String value, DeviceRgb bg, PdfFont font, float fontSize) {
        Cell cell = new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(fontSize))
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
        if (bg != null) cell.setBackgroundColor(bg);
        return cell;
    }
}