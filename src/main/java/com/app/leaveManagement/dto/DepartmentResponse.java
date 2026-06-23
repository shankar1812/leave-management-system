package com.app.leaveManagement.dto;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentResponse {
	private Long id;
    private String name;
    private LocalTime shiftStartTime;
}
