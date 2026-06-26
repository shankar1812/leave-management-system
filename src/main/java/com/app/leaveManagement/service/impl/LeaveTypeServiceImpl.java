package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.dto.LeaveTypeRequest;
import com.app.leaveManagement.dto.LeaveTypeResponse;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.exception.DuplicateResourceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.service.LeaveTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveTypeServiceImpl implements LeaveTypeService {

    private final LeaveTypeRepository leaveTypeRepository;

    @Override
    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
        log.info("Creating leave type with name: {}", request.getName());

        if (leaveTypeRepository.existsByName(request.getName())) {
            log.warn("Leave type already exists with name: {}", request.getName());
            throw new DuplicateResourceException(
                "Leave type already exists with name: " + request.getName()
            );
        }

        LeaveType leaveType = LeaveType.builder()
                .name(request.getName())
                .maxDaysPerYear(request.getMaxDaysPerYear())
                .isCarryForwardAllowed(request.getIsCarryForwardAllowed())
                .isActive(true)
                .build();

        LeaveType saved = leaveTypeRepository.save(leaveType);
        log.info("Leave type created successfully with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public LeaveTypeResponse getLeaveTypeById(Long id) {
        log.info("Fetching leave type with id: {}", id);
        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave type not found with id: " + id
                ));
        return mapToResponse(leaveType);
    }

    @Override
    public List<LeaveTypeResponse> getAllActiveLeaveTypes() {
        log.info("Fetching all active leave types");
        return leaveTypeRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest request) {
        log.info("Updating leave type with id: {}", id);

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave type not found with id: " + id
                ));

        if (!leaveType.getName().equals(request.getName()) &&
                leaveTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                "Leave type already exists with name: " + request.getName()
            );
        }

        leaveType.setName(request.getName());
        leaveType.setMaxDaysPerYear(request.getMaxDaysPerYear());
        leaveType.setCarryForwardAllowed(request.getIsCarryForwardAllowed());

        LeaveType updated = leaveTypeRepository.save(leaveType);
        log.info("Leave type updated successfully with id: {}", updated.getId());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateLeaveType(Long id) {
        log.info("Deactivating leave type with id: {}", id);

        LeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave type not found with id: " + id
                ));

        leaveType.setActive(false);
        leaveTypeRepository.save(leaveType);
        log.info("Leave type deactivated successfully with id: {}", id);
    }

    private LeaveTypeResponse mapToResponse(LeaveType leaveType) {
        return LeaveTypeResponse.builder()
                .id(leaveType.getId())
                .name(leaveType.getName())
                .maxDaysPerYear(leaveType.getMaxDaysPerYear())
                .isCarryForwardAllowed(leaveType.isCarryForwardAllowed())
                .isActive(leaveType.isActive())
                .build();
    }
}