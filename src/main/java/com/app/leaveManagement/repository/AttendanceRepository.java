package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.AttendanceRecord;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByUserIdAndDate(Long userId, LocalDate date);

    List<AttendanceRecord> findByUserIdAndDateBetweenOrderByDateAsc(
        Long userId, LocalDate startDate, LocalDate endDate
    );

    // Used by absent detection job – finds all users who have no record for a given date
    @Query("""
        SELECT u FROM User u
        WHERE u.isActive = true
        AND u.id NOT IN (
            SELECT ar.user.id FROM AttendanceRecord ar
            WHERE ar.date = :date
        )
    """)
    List<User> findUsersWithNoAttendanceForDate(@Param("date") LocalDate date);

    // Monthly summary counts
    @Query("""
        SELECT COUNT(ar) FROM AttendanceRecord ar
        WHERE ar.user.id = :userId
        AND ar.date BETWEEN :startDate AND :endDate
        AND ar.status = :status
    """)
    Long countByUserIdAndDateBetweenAndStatus(
        @Param("userId") Long userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("status") AttendanceStatus status
    );
}