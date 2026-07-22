package com.app.leaveManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeaveManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaveManagementSystemApplication.class, args);
	}

}
