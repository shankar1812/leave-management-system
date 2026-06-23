package com.app.leaveManagement.service;


import com.app.leaveManagement.dto.DepartmentRequest;
import com.app.leaveManagement.dto.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {
	
    DepartmentResponse createDepartment(DepartmentRequest request);
    DepartmentResponse getDepartmentById(Long id);
    Page<DepartmentResponse> getAllDepartments(Pageable pageable);
    DepartmentResponse updateDepartment(Long id, DepartmentRequest request);
    void deleteDepartment(Long id);
    
}
