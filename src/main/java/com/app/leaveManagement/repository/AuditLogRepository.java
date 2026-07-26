package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType, Long entityId
    );
    List<AuditLog> findByPerformedByOrderByCreatedAtDesc(String performedBy);
}