package com.app.leaveManagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.leaveManagement.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
	boolean existsByName(String name);
}
