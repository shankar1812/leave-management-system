package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.LeaveApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {

    List<LeaveApproval> findByLeaveApplicationId(Long leaveApplicationId);

    Optional<LeaveApproval> findByLeaveApplicationIdAndApprovalLevel(
        Long leaveApplicationId, Integer approvalLevel
    );
}