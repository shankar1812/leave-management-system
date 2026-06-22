package com.app.leaveManagement.service;

import java.awt.print.Pageable;

import org.springframework.data.domain.Page;

import com.app.leaveManagement.dto.UserRegistrationRequest;
import com.app.leaveManagement.dto.UserResponse;
import com.app.leaveManagement.dto.UserUpdateRequest;





public interface UserService {

    UserResponse registerUser(UserRegistrationRequest request);

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    Page <UserResponse> getAllUsers(org.springframework.data.domain.Pageable pageable);
}