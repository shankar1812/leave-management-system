package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.HolidayRequest;
import com.app.leaveManagement.dto.HolidayResponse;

import java.util.List;

public interface HolidayService {
	
    HolidayResponse createHoliday(HolidayRequest request);
    HolidayResponse getHolidayById(Long id);
    List<HolidayResponse> getAllHolidays(Integer year);
    HolidayResponse updateHoliday(Long id, HolidayRequest request);
    void deleteHoliday(Long id);
}