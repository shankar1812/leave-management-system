package com.app.leaveManagement.event;



import com.app.leaveManagement.enums.LeaveStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LeaveStatusChangedEvent extends ApplicationEvent {

    private final Long leaveApplicationId;
    private final Long userId;
    private final String userEmail;
    private final String userName;
    private final LeaveStatus previousStatus;
    private final LeaveStatus newStatus;
    private final String remarks;

    public LeaveStatusChangedEvent(
            Object source,
            Long leaveApplicationId,
            Long userId,
            String userEmail,
            String userName,
            LeaveStatus previousStatus,
            LeaveStatus newStatus,
            String remarks) {
        super(source);
        this.leaveApplicationId = leaveApplicationId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.remarks = remarks;
    }
}
