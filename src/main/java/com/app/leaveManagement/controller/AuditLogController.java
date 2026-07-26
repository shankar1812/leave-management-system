package com.app.leaveManagement.controller;

import com.app.leaveManagement.entity.AuditLog;
import com.app.leaveManagement.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/entity/{type}/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<AuditLog>> getByEntity(
            @PathVariable String type,
            @PathVariable Long id) {
        return ResponseEntity.ok(
            auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(type, id)
        );
    }

    @GetMapping("/user/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<AuditLog>> getByUser(@PathVariable String email) {
        return ResponseEntity.ok(
            auditLogRepository.findByPerformedByOrderByCreatedAtDesc(email)
        );
    }
}