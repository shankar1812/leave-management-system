package com.app.leaveManagement.dto;

import java.time.LocalDate;

import com.app.leaveManagement.enums.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String departmentName;
    private LocalDate joiningDate;
    private boolean isActive;
}
