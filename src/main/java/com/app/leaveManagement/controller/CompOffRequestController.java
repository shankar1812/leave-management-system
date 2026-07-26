package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.CompOffDecisionRequest;
import com.app.leaveManagement.dto.CompOffRequestDTO;
import com.app.leaveManagement.dto.CompOffRequestResponse;
import com.app.leaveManagement.enums.CompOffStatus;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.CompOffRequestService;
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
@RequestMapping("/api/v1/comp-off")
@RequiredArgsConstructor
@Tag(name = "Comp-off Requests",
     description = "Compensatory leave for working on holidays or weekends")
public class CompOffRequestController {

    private final CompOffRequestService compOffRequestService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
        summary = "Submit comp-off request",
        description = "Employee claims comp-off for a holiday or weekend they worked on."
    )
    public ResponseEntity<CompOffRequestResponse> submitCompOffRequest(
            @Valid @RequestBody CompOffRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return new ResponseEntity<>(
            compOffRequestService.submitCompOffRequest(userId, request),
            HttpStatus.CREATED
        );
    }

    @PostMapping("/{id}/decision")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Manager approves or rejects comp-off request",
        description = "On approval, 1 comp-off day is credited to employee's leave balance."
    )
    public ResponseEntity<CompOffRequestResponse> processDecision(
            @PathVariable Long id,
            @Valid @RequestBody CompOffDecisionRequest decision,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = getUserId(userDetails);
        return ResponseEntity.ok(
            compOffRequestService.processManagerDecision(managerId, id, decision)
        );
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my comp-off requests")
    public ResponseEntity<Page<CompOffRequestResponse>> getMyCompOffRequests(
            @RequestParam(required = false) CompOffStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(
            compOffRequestService.getMyCompOffRequests(userId, status, pageable)
        );
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Manager views team comp-off requests")
    public ResponseEntity<Page<CompOffRequestResponse>> getTeamCompOffRequests(
            @RequestParam(required = false) CompOffStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {

        Long managerId = getUserId(userDetails);
        return ResponseEntity.ok(
            compOffRequestService.getTeamCompOffRequests(managerId, status, pageable)
        );
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}