package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.LeaveBalanceResponse;
import com.app.leaveManagement.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER') or #userId == authentication.principal.id")
    public ResponseEntity<List<LeaveBalanceResponse>> getBalancesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveBalanceService.getBalancesByUser(userId));
    }
}