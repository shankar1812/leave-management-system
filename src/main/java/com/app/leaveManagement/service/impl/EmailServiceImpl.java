package com.app.leaveManagement.service.impl;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.app.leaveManagement.event.LeaveStatusChangedEvent;
import com.app.leaveManagement.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {


    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendLeaveStatusNotification(LeaveStatusChangedEvent event) {
        log.info("Sending leave status email to: {} for leave id: {}",
                event.getUserEmail(), event.getLeaveApplicationId());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(event.getUserEmail());
            message.setSubject(buildSubject(event));
            message.setText(buildBody(event));
            mailSender.send(message);

            log.info("Email sent successfully to: {}", event.getUserEmail());
        } catch (Exception e) {
            // Never let email failure break the main flow
            log.error("Failed to send email to: {} — {}",
                    event.getUserEmail(), e.getMessage());
        }
    }

    private String buildSubject(LeaveStatusChangedEvent event) {
        return String.format("Leave Application %s — Leave Management System",
                event.getNewStatus().name());
    }

    private String buildBody(LeaveStatusChangedEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(event.getUserName()).append(",\n\n");

        switch (event.getNewStatus()) {
            case APPROVED -> body.append("Your leave application has been approved.\n");
            case REJECTED -> body.append("Your leave application has been rejected.\n");
            case CANCELLED -> body.append("Your leave application has been cancelled.\n");
            default -> body.append("Your leave application status has been updated to: ")
                    .append(event.getNewStatus().name()).append(".\n");
        }

        if (event.getRemarks() != null && !event.getRemarks().isBlank()) {
            body.append("\nRemarks: ").append(event.getRemarks()).append("\n");
        }

        body.append("\nLeave Application ID: ").append(event.getLeaveApplicationId());
        body.append("\n\nRegards,\nLeave Management System");
        return body.toString();
    }
}