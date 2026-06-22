package com.app.leaveManagement.service.impl;



import com.app.leaveManagement.dto.LoginRequest;
import com.app.leaveManagement.dto.LoginResponse;
import com.app.leaveManagement.security.JwtService;
import com.app.leaveManagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service                              // ← CRITICAL: This makes it a Spring bean
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    
    public AuthServiceImpl(AuthenticationManager authenticationManager ,JwtService jwtService) {
    	this.authenticationManager =authenticationManager;
    	this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return LoginResponse.builder()
                .token(token)
                .email(userDetails.getUsername())
                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                .build();
    }
}
