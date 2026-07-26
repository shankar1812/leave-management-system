package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.config.CacheConfig;
import com.app.leaveManagement.dto.HolidayRequest;
import com.app.leaveManagement.dto.HolidayResponse;
import com.app.leaveManagement.entity.Holiday;
import com.app.leaveManagement.exception.DuplicateResourceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.HolidayRepository;
import com.app.leaveManagement.service.HolidayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayServiceImpl implements HolidayService {

    private final HolidayRepository holidayRepository;

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.HOLIDAYS_CACHE, allEntries = true)
    @Auditable(action = "CREATE_HOLIDAY", entityType = "Holiday")
    public HolidayResponse createHoliday(HolidayRequest request) {
        log.info("Creating holiday: {} on date: {}", request.getName(), request.getDate());

        if (holidayRepository.existsByDateAndName(request.getDate(), request.getName())) {
            throw new DuplicateResourceException(
                "Holiday already exists with same name and date: " + request.getDate()
            );
        }

        Holiday holiday = Holiday.builder()
                .name(request.getName())
                .date(request.getDate())
                .type(request.getType())
                .build();

        Holiday saved = holidayRepository.save(holiday);
        log.info("Holiday created with id: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public HolidayResponse getHolidayById(Long id) {
        log.info("Fetching holiday with id: {}", id);
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Holiday not found with id: " + id
                ));
        return mapToResponse(holiday);
    }

    @Override
    @Cacheable(value = CacheConfig.HOLIDAYS_CACHE, key = "#year")
    public List<HolidayResponse> getAllHolidays(Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        log.info("CACHE MISS — fetching holidays from DB for year: {}", targetYear);

        return holidayRepository.findByYear(targetYear)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.HOLIDAYS_CACHE, allEntries = true)
    @Auditable(action = "UPDATE_HOLIDAY", entityType = "Holiday")
    public HolidayResponse updateHoliday(Long id, HolidayRequest request) {
        log.info("Updating holiday id: {}", id);

        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Holiday not found with id: " + id
                ));

        holiday.setName(request.getName());
        holiday.setDate(request.getDate());
        holiday.setType(request.getType());

        return mapToResponse(holidayRepository.save(holiday));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.HOLIDAYS_CACHE, allEntries = true)
    @Auditable(action = "DELETE_HOLIDAY", entityType = "Holiday")
    public void deleteHoliday(Long id) {
        log.info("Deleting holiday id: {}", id);

        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Holiday not found with id: " + id
                ));

        holidayRepository.delete(holiday);
        log.info("Holiday deleted id: {}", id);
    }

    private HolidayResponse mapToResponse(Holiday holiday) {
        return HolidayResponse.builder()
                .id(holiday.getId())
                .name(holiday.getName())
                .date(holiday.getDate())
                .type(holiday.getType())
                .build();
    }
}