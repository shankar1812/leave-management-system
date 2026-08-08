package com.app.leaveManagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Leave Management System API")
                        .description("""
                            Enterprise HR platform API covering:
                            - JWT Authentication & Role-Based Access Control
                            - Leave Types, Balances, Applications & Multi-level Approval
                            - Attendance Tracking with Clock-in / Clock-out
                            - Holiday Calendar, WFH & Comp-off Modules
                            - PDF Report Generation
                            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Shankar Sahu")
                                .email("sankarsahu4043@gmail.com")
                                .url("https://github.com/shankar1812")))
                
                .servers(List.of(
                        new Server()
                            .url("https://leave-management-system-production-5147.up.railway.app")
                            .description("Production — Railway.app"),
                        new Server()
                            .url("http://localhost:8080")
                            .description("Local Development")
                    ))
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token. Obtain it from POST /api/v1/auth/login")
                        ));
    }
}