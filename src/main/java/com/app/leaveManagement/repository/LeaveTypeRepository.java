package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    boolean existsByName(String name);
    List<LeaveType> findByIsActiveTrue();
}