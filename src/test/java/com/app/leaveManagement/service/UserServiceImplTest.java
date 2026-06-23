package com.app.leaveManagement.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.leaveManagement.dto.UserRegistrationRequest;
import com.app.leaveManagement.dto.UserResponse;
import com.app.leaveManagement.dto.UserUpdateRequest;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.exception.DuplicateResourceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.DepartmentRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    // ---------- registerUser ----------

    @Test
    void shouldRegisterUserSuccessfully() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setName("Shankar Sahu");
        request.setEmail("shankar@example.com");
        request.setPassword("password123");
        request.setRole(Role.EMPLOYEE);

        when(userRepository.findByEmail("shankar@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals("Shankar Sahu", response.getName());
        assertEquals("shankar@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void shouldThrowWhenEmailAlreadyExistsDuringRegistration() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("existing@example.com");

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(DuplicateResourceException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowWhenDepartmentNotFoundDuringRegistration() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("new@example.com");
        request.setDepartmentId(99L);

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.registerUser(request));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- getUserById ----------

    @Test
    void shouldGetUserByIdSuccessfully() {
        User user = User.builder()
                .id(1L)
                .name("Shankar Sahu")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .isActive(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertEquals("Shankar Sahu", response.getName());
        assertEquals(1L, response.getId());
    }

    @Test
    void shouldThrowWhenUserNotFoundById() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    // ---------- updateUser ----------

    @Test
    void shouldUpdateUserSuccessfully() {
        User existingUser = User.builder()
                .id(1L)
                .name("Old Name")
                .email("shankar@example.com")
                .role(Role.EMPLOYEE)
                .isActive(true)
                .build();

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(1L, request);

        assertEquals("Updated Name", response.getName());
        verify(userRepository).save(existingUser);
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("Doesn't Matter");

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(99L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUpdatingWithInvalidDepartment() {
        User existingUser = User.builder().id(1L).name("Shankar").build();
        UserUpdateRequest request = new UserUpdateRequest();
        request.setDepartmentId(99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(1L, request));
    }

    // ---------- getAllUsers ----------

    @Test
    void shouldGetAllUsersWithPagination() {
        User user1 = User.builder().id(1L).name("User One").role(Role.EMPLOYEE).build();
        User user2 = User.builder().id(2L).name("User Two").role(Role.MANAGER).build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user1, user2), pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("User One", result.getContent().get(0).getName());
    }
}