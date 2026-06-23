package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.dto.DepartmentRequest;
import com.app.leaveManagement.dto.DepartmentResponse;
import com.app.leaveManagement.entity.Department;
import com.app.leaveManagement.exception.DuplicateResourceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.DepartmentRepository;
import com.app.leaveManagement.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        log.info("Creating department with name: {}", request.getName());

        if (departmentRepository.existsByName(request.getName())) {
            log.warn("Department already exists with name: {}", request.getName());
            throw new DuplicateResourceException("Department already exists with name: " + request.getName());
        }

        Department department = Department.builder()
                .name(request.getName())
                .shiftStartTime(request.getShiftStartTime())
                .build();

        Department saved = departmentRepository.save(department);
        log.info("Department created successfully with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        log.info("Fetching department with id: {}", id);
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return mapToResponse(department);
    }

    @Override
    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {
        log.info("Fetching all departments - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return departmentRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        log.info("Updating department with id: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!department.getName().equals(request.getName()) &&
                departmentRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Department already exists with name: " + request.getName());
        }

        department.setName(request.getName());
        department.setShiftStartTime(request.getShiftStartTime());

        Department updated = departmentRepository.save(department);
        log.info("Department updated successfully with id: {}", updated.getId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        log.info("Deleting department with id: {}", id);

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        departmentRepository.delete(department);
        log.info("Department deleted successfully with id: {}", id);
    }

    private DepartmentResponse mapToResponse(Department department) {
        return DepartmentResponse.builder()
                .id(department.getId())
                .name(department.getName())
                .shiftStartTime(department.getShiftStartTime())
                .build();
    }
}