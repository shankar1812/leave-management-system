package com.app.leaveManagement.dto;



import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String email;
    private String role;
}
