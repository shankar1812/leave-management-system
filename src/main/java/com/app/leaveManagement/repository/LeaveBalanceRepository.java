package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(
        Long userId, Long leaveTypeId, Integer year
    );

    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);
}