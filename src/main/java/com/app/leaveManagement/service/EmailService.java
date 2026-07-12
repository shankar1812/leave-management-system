package com.app.leaveManagement.service;

import com.app.leaveManagement.event.LeaveStatusChangedEvent;

public interface EmailService {
    void sendLeaveStatusNotification(LeaveStatusChangedEvent event);
}