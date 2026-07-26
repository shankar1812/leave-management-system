package com.app.leaveManagement.service;



import com.app.leaveManagement.dto.LoginRequest;
import com.app.leaveManagement.dto.LoginResponse;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.security.JwtService;
import com.app.leaveManagement.service.impl.AuthServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("shankar@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .name("Shankar Sahu")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .build();

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("shankar@example.com")
                .password("encoded")
                .authorities(List.of())
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("shankar@example.com"))
                .thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername("shankar@example.com"))
                .thenReturn(userDetails);
        when(jwtService.generateToken(userDetails))
                .thenReturn("mocked.jwt.token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("shankar@example.com", response.getEmail());
        assertEquals("ROLE_EMPLOYEE", response.getRole());
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void shouldThrowWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@example.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void shouldThrowWhenUserNotFoundAfterAuth() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
    }
}