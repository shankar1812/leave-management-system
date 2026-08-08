package com.app.leaveManagement.config;

import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.enums.Role;
import com.app.leaveManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@lms.com").isEmpty()) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@lms.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .joiningDate(LocalDate.now())
                    .build();
            userRepository.save(admin);
            log.info("✅ Admin user seeded: admin@lms.com / Admin@1234");
        } else {
            log.info("ℹ️ Admin already exists, skipping seed.");
        }
    }
}