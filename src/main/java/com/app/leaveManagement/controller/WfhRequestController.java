package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.WfhDecisionRequest;
import com.app.leaveManagement.dto.WfhRequestDTO;
import com.app.leaveManagement.dto.WfhRequestResponse;
import com.app.leaveManagement.enums.WfhStatus;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.WfhRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/wfh")
@RequiredArgsConstructor
@Tag(name = "WFH Requests", description = "Work From Home request management")
public class WfhRequestController {

    private final WfhRequestService wfhRequestService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Submit WFH request", description = "Employee submits a WFH request for a specific date.")
    public ResponseEntity<WfhRequestResponse> submitWfhRequest(
            @Valid @RequestBody WfhRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return new ResponseEntity<>(
            wfhRequestService.submitWfhRequest(userId, request),
            HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager approves or rejects WFH request")
    public ResponseEntity<WfhRequestResponse> processDecision(
            @PathVariable Long id,
            @Valid @RequestBody WfhDecisionRequest decision,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = getUserId(userDetails);
        return ResponseEntity.ok(
            wfhRequestService.processManagerDecision(managerId, id, decision)
        );
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Cancel a PENDING WFH request")
    public ResponseEntity<WfhRequestResponse> cancelWfhRequest(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(wfhRequestService.cancelWfhRequest(userId, id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my WFH requests with optional status filter")
    public ResponseEntity<Page<WfhRequestResponse>> getMyWfhRequests(
            @RequestParam(required = false) WfhStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(
            wfhRequestService.getMyWfhRequests(userId, status, pageable)
        );
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager views team WFH requests")
    public ResponseEntity<Page<WfhRequestResponse>> getTeamWfhRequests(
            @RequestParam(required = false) WfhStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = getUserId(userDetails);
        return ResponseEntity.ok(
            wfhRequestService.getTeamWfhRequests(managerId, status, pageable)
        );
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}