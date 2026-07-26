package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.WfhRequest;
import com.app.leaveManagement.enums.WfhStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface WfhRequestRepository extends JpaRepository<WfhRequest, Long> {

    Page<WfhRequest> findByUserId(Long userId, Pageable pageable);

    Page<WfhRequest> findByUserIdAndStatus(Long userId, WfhStatus status, Pageable pageable);

    // Manager sees all WFH requests from their direct reports
    Page<WfhRequest> findByUserManagerId(Long managerId, Pageable pageable);

    Page<WfhRequest> findByUserManagerIdAndStatus(Long managerId, WfhStatus status, Pageable pageable);

    // Prevent duplicate WFH request for same date
    Optional<WfhRequest> findByUserIdAndDate(Long userId, LocalDate date);
}