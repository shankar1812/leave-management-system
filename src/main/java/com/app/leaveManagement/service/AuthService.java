package com.app.leaveManagement.service;


import com.app.leaveManagement.dto.LoginRequest;
import com.app.leaveManagement.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
