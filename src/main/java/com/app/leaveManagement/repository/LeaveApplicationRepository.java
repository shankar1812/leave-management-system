package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.LeaveApplication;
import com.app.leaveManagement.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    List<LeaveApplication> findByUserId(Long userId);

    List<LeaveApplication> findByUserIdAndStatus(Long userId, LeaveStatus status);

    @Query("""
        SELECT la FROM LeaveApplication la
        WHERE la.user.id = :userId
        AND la.status NOT IN ('REJECTED', 'CANCELLED')
        AND (la.startDate <= :endDate AND la.endDate >= :startDate)
    """)
    List<LeaveApplication> findOverlappingLeaves(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}