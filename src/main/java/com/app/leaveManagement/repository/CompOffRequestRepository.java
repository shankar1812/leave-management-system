package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.CompOffRequest;
import com.app.leaveManagement.enums.CompOffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CompOffRequestRepository extends JpaRepository<CompOffRequest, Long> {

    Page<CompOffRequest> findByUserId(Long userId, Pageable pageable);

    Page<CompOffRequest> findByUserIdAndStatus(
        Long userId, CompOffStatus status, Pageable pageable
    );

    Page<CompOffRequest> findByUserManagerId(Long managerId, Pageable pageable);

    Page<CompOffRequest> findByUserManagerIdAndStatus(
        Long managerId, CompOffStatus status, Pageable pageable
    );

    Optional<CompOffRequest> findByUserIdAndWorkedOnDate(Long userId, LocalDate workedOnDate);
}