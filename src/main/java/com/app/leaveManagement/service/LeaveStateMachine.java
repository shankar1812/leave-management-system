package com.app.leaveManagement.service;

import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class LeaveStateMachine {

    private static final Map<LeaveStatus, Set<LeaveStatus>> VALID_TRANSITIONS = Map.of(
        LeaveStatus.PENDING,   Set.of(LeaveStatus.APPROVED, LeaveStatus.REJECTED, LeaveStatus.CANCELLED),
        LeaveStatus.APPROVED,  Set.of(LeaveStatus.CANCELLED),
        LeaveStatus.REJECTED,  Set.of(),
        LeaveStatus.CANCELLED, Set.of()
    );

    public void validateTransition(LeaveStatus current, LeaveStatus next) {
        Set<LeaveStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot transition leave status from %s to %s", current, next)
            );
        }
    }

    public boolean canTransition(LeaveStatus current, LeaveStatus next) {
        return VALID_TRANSITIONS.getOrDefault(current, Set.of()).contains(next);
    }
}