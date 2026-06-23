package com.app.leaveManagement.controller;

import com.app.leaveManagement.dto.DepartmentRequest;
import com.app.leaveManagement.dto.DepartmentResponse;
import com.app.leaveManagement.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return new ResponseEntity<>(departmentService.createDepartment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Page<DepartmentResponse>> getAllDepartments(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(departmentService.getAllDepartments(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}