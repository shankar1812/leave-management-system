package com.app.leaveManagement.repository;

import com.app.leaveManagement.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    boolean existsByName(String name);
    List<LeaveType> findByIsActiveTrue();
    Optional<LeaveType> findByName(String name);
}