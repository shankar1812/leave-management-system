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
        // Check if admin already exists
        if (userRepository.findByEmail("admin@example.com").isEmpty()) {
            User admin = User.builder()
                    .name("System Admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .joiningDate(LocalDate.now())
                    .build();
            userRepository.save(admin);
            log.info("✅ Admin user seeded: admin@example.com / admin123");
        } else {
            log.info("ℹ️ Admin already exists, skipping seed.");
        }
    }
}
