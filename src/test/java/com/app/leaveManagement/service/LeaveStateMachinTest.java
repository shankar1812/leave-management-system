package com.app.leaveManagement.service;

import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeaveStateMachineTest {

    private final LeaveStateMachine stateMachine = new LeaveStateMachine();

    @Test
    void shouldAllowPendingToApproved() {
        assertDoesNotThrow(() ->
            stateMachine.validateTransition(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );
    }

    @Test
    void shouldAllowPendingToRejected() {
        assertDoesNotThrow(() ->
            stateMachine.validateTransition(LeaveStatus.PENDING, LeaveStatus.REJECTED)
        );
    }

    @Test
    void shouldAllowPendingToCancelled() {
        assertDoesNotThrow(() ->
            stateMachine.validateTransition(LeaveStatus.PENDING, LeaveStatus.CANCELLED)
        );
    }

    @Test
    void shouldAllowApprovedToCancelled() {
        assertDoesNotThrow(() ->
            stateMachine.validateTransition(LeaveStatus.APPROVED, LeaveStatus.CANCELLED)
        );
    }

    @Test
    void shouldRejectCancelledToApproved() {
        assertThrows(InvalidStateTransitionException.class, () ->
            stateMachine.validateTransition(LeaveStatus.CANCELLED, LeaveStatus.APPROVED)
        );
    }

    @Test
    void shouldRejectRejectedToApproved() {
        assertThrows(InvalidStateTransitionException.class, () ->
            stateMachine.validateTransition(LeaveStatus.REJECTED, LeaveStatus.APPROVED)
        );
    }

    @Test
    void shouldRejectApprovedToRejected() {
        assertThrows(InvalidStateTransitionException.class, () ->
            stateMachine.validateTransition(LeaveStatus.APPROVED, LeaveStatus.REJECTED)
        );
    }

    @Test
    void canTransitionShouldReturnTrueForValidTransition() {
        assertTrue(stateMachine.canTransition(LeaveStatus.PENDING, LeaveStatus.APPROVED));
    }

    @Test
    void canTransitionShouldReturnFalseForInvalidTransition() {
        assertFalse(stateMachine.canTransition(LeaveStatus.REJECTED, LeaveStatus.APPROVED));
    }
}