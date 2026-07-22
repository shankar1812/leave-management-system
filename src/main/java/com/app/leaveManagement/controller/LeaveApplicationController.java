package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.LeaveApplicationRequest;
import com.app.leaveManagement.dto.LeaveApplicationResponse;
import com.app.leaveManagement.enums.LeaveStatus;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Leave Applications", description = "Employee leave application, cancellation, and history")
public class LeaveApplicationController {

    private final LeaveApplicationService leaveApplicationService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Apply for leave",
        description = "Employee submits a leave application. Validates overlap, balance, and active leave type."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Leave application created successfully"),
        @ApiResponse(responseCode = "400", description = "Insufficient balance or invalid date range"),
        @ApiResponse(responseCode = "409", description = "Overlapping leave exists or invalid state transition")
    })
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
    @Operation(
        summary = "Cancel a leave application",
        description = "Employee cancels their own PENDING leave. Balance is restored automatically."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leave cancelled successfully"),
        @ApiResponse(responseCode = "404", description = "Leave application not found"),
        @ApiResponse(responseCode = "409", description = "Cannot cancel leave in current status")
    })
    public ResponseEntity<LeaveApplicationResponse> cancelLeave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(leaveApplicationService.cancelLeave(userId, id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(
        summary = "Get leave application by ID",
        description = "Fetch a specific leave application by its ID. Accessible to all authenticated users."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leave found"),
        @ApiResponse(responseCode = "404", description = "Leave application not found")
    })
    public ResponseEntity<LeaveApplicationResponse> getLeaveById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveApplicationService.getLeaveById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Get my leaves",
        description = "Get all leaves for the currently authenticated employee with optional status filter."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leaves fetched successfully")
    })
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
    @Operation(
        summary = "Get team leaves",
        description = "Manager gets all leaves of their direct reports with optional status filter."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team leaves fetched successfully"),
        @ApiResponse(responseCode = "403", description = "Only MANAGER role can access this endpoint")
    })
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