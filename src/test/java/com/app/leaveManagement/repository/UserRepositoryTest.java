package com.app.leaveManagement.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.Role;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .role(Role.EMPLOYEE)
                .isActive(true)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        System.out.println("Found: " + found); 
        assertTrue(found.isPresent());
		assertEquals("Test User", found.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nobody@example.com");
        assertTrue(found.isEmpty());
    }
}
