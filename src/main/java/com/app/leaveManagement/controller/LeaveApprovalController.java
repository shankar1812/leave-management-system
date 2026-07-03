package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.LeaveApprovalRequest;
import com.app.leaveManagement.dto.LeaveApprovalResponse;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leaves/{leaveId}/approvals")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;
    private final UserRepository userRepository;

    @PostMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LeaveApprovalResponse> managerDecision(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = getUserId(userDetails);
        return ResponseEntity.ok(
            leaveApprovalService.processManagerDecision(managerId, leaveId, request)
        );
    }

    @PostMapping("/hr")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<LeaveApprovalResponse> hrDecision(
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long hrUserId = getUserId(userDetails);
        return ResponseEntity.ok(
            leaveApprovalService.processHRDecision(hrUserId, leaveId, request)
        );
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}