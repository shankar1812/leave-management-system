package com.app.leaveManagement.service;

import com.app.leaveManagement.dto.LeaveBalanceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LeaveBalanceService {

    void initializeBalancesForUser(Long userId);

    void creditYearlyBalances(Integer year);

    List<LeaveBalanceResponse> getBalancesByUser(Long userId);

    void deductBalance(Long userId, Long leaveTypeId, BigDecimal days);

    void restoreBalance(Long userId, Long leaveTypeId, BigDecimal days);
}