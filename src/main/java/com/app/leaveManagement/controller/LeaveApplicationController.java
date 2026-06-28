package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.LeaveApplicationRequest;
import com.app.leaveManagement.dto.LeaveApplicationResponse;
import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<LeaveApplicationResponse> applyLeave(
            @Valid @RequestBody LeaveApplicationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return new ResponseEntity<>(
            leaveApplicationService.applyLeave(userId, request),
            HttpStatus.CREATED
        );
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<LeaveApplicationResponse> cancelLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(leaveApplicationService.cancelLeave(userId, id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<LeaveApplicationResponse> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveApplicationService.getLeaveById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<LeaveApplicationResponse>> getMyLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 10, sort = "appliedAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(
            leaveApplicationService.getLeavesByUser(userId, status, pageable)
        );
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Page<LeaveApplicationResponse>> getTeamLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 10, sort = "appliedAt") Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = getUserId(userDetails);
        return ResponseEntity.ok(
            leaveApplicationService.getLeavesByManager(managerId, status, pageable)
        );
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}