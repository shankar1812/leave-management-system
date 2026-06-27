package com.app.leaveManagement.service.impl;

import com.app.leaveManagement.audit.Auditable;
import com.app.leaveManagement.dto.LeaveBalanceResponse;
import com.app.leaveManagement.entity.LeaveBalance;
import com.app.leaveManagement.entity.LeaveType;
import com.app.leaveManagement.entity.User;
import com.app.leaveManagement.exception.InsufficientLeaveBalanceException;
import com.app.leaveManagement.exception.ResourceNotFoundException;
import com.app.leaveManagement.repository.LeaveBalanceRepository;
import com.app.leaveManagement.repository.LeaveTypeRepository;
import com.app.leaveManagement.repository.UserRepository;
import com.app.leaveManagement.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @Auditable(action = "INITIALIZE_BALANCE", entityType = "LeaveBalance")
    public void initializeBalancesForUser(Long userId) {
        log.info("Initializing leave balances for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "User not found with id: " + userId
                ));

        List<LeaveType> activeLeaveTypes = leaveTypeRepository.findByIsActiveTrue();
        int currentYear = LocalDate.now().getYear();

        for (LeaveType leaveType : activeLeaveTypes) {
            boolean alreadyExists = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveType.getId(), currentYear)
                .isPresent();

            if (!alreadyExists) {
                LeaveBalance balance = LeaveBalance.builder()
                        .user(user)
                        .leaveType(leaveType)
                        .year(currentYear)
                        .totalDays(BigDecimal.valueOf(leaveType.getMaxDaysPerYear()))
                        .usedDays(BigDecimal.ZERO)
                        .remainingDays(BigDecimal.valueOf(leaveType.getMaxDaysPerYear()))
                        .build();

                leaveBalanceRepository.save(balance);
                log.info("Initialized {} balance: {} days for user id: {}",
                    leaveType.getName(), leaveType.getMaxDaysPerYear(), userId);
            }
        }
    }

    @Override
    @Transactional
    public void creditYearlyBalances(Integer year) {
        log.info("Crediting yearly leave balances for year: {}", year);

        List<User> activeUsers = userRepository.findByIsActiveTrue();
        List<LeaveType> activeLeaveTypes = leaveTypeRepository.findByIsActiveTrue();

        for (User user : activeUsers) {
            for (LeaveType leaveType : activeLeaveTypes) {
                Optional<LeaveBalance> existing = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeIdAndYear(user.getId(), leaveType.getId(), year);

                if (existing.isEmpty()) {
                    BigDecimal carryForwardDays = BigDecimal.ZERO;

                    // Carry forward unused earned leave from previous year
                    if (leaveType.isCarryForwardAllowed()) {
                        Optional<LeaveBalance> previousYear = leaveBalanceRepository
                            .findByUserIdAndLeaveTypeIdAndYear(
                                user.getId(), leaveType.getId(), year - 1
                            );
                        if (previousYear.isPresent()) {
                            carryForwardDays = previousYear.get().getRemainingDays();
                            log.info("Carrying forward {} days of {} for user id: {}",
                                carryForwardDays, leaveType.getName(), user.getId());
                        }
                    }

                    BigDecimal totalDays = BigDecimal.valueOf(leaveType.getMaxDaysPerYear())
                            .add(carryForwardDays);

                    LeaveBalance balance = LeaveBalance.builder()
                            .user(user)
                            .leaveType(leaveType)
                            .year(year)
                            .totalDays(totalDays)
                            .usedDays(BigDecimal.ZERO)
                            .remainingDays(totalDays)
                            .build();

                    leaveBalanceRepository.save(balance);
                }
            }
        }

        log.info("Yearly balance credit completed for year: {}", year);
    }

    @Override
    public List<LeaveBalanceResponse> getBalancesByUser(Long userId) {
        log.info("Fetching leave balances for user id: {}", userId);

        int currentYear = LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository
                .findByUserIdAndYear(userId, currentYear);

        return balances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(action = "DEDUCT_BALANCE", entityType = "LeaveBalance")
    public void deductBalance(Long userId, Long leaveTypeId, BigDecimal days) {
        log.info("Deducting {} days from leave type id: {} for user id: {}",
                days, leaveTypeId, userId);

        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, currentYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave balance not found for user id: " + userId
                ));

        if (balance.getRemainingDays().compareTo(days) < 0) {
            log.warn("Insufficient balance. Required: {}, Available: {}",
                    days, balance.getRemainingDays());
            throw new InsufficientLeaveBalanceException(
                "Insufficient leave balance. Required: " + days +
                " days, Available: " + balance.getRemainingDays() + " days"
            );
        }

        balance.setUsedDays(balance.getUsedDays().add(days));
        balance.setRemainingDays(balance.getRemainingDays().subtract(days));
        leaveBalanceRepository.save(balance);

        log.info("Balance deducted successfully. Remaining: {}", balance.getRemainingDays());
    }

    @Override
    @Transactional
    @Auditable(action = "RESTORE_BALANCE", entityType = "LeaveBalance")
    public void restoreBalance(Long userId, Long leaveTypeId, BigDecimal days) {
        log.info("Restoring {} days to leave type id: {} for user id: {}",
                days, leaveTypeId, userId);

        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, currentYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Leave balance not found for user id: " + userId
                ));

        balance.setUsedDays(balance.getUsedDays().subtract(days));
        balance.setRemainingDays(balance.getRemainingDays().add(days));
        leaveBalanceRepository.save(balance);

        log.info("Balance restored successfully. Remaining: {}", balance.getRemainingDays());
    }

    private LeaveBalanceResponse mapToResponse(LeaveBalance balance) {
        return LeaveBalanceResponse.builder()
                .id(balance.getId())
                .userId(balance.getUser().getId())
                .userName(balance.getUser().getName())
                .leaveTypeId(balance.getLeaveType().getId())
                .leaveTypeName(balance.getLeaveType().getName())
                .year(balance.getYear())
                .totalDays(balance.getTotalDays())
                .usedDays(balance.getUsedDays())
                .remainingDays(balance.getRemainingDays())
                .build();
    }
}