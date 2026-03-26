package com.example.lms_backend.service;

import com.example.lms_backend.dto.LeaveBalanceResponse;
import com.example.lms_backend.entity.LeaveBalance;
import com.example.lms_backend.entity.LeaveType;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.exception.InsufficientBalanceException;
import com.example.lms_backend.repository.LeaveBalanceRepository;
import com.example.lms_backend.repository.LeaveTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               LeaveTypeRepository leaveTypeRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    /**
     * Initialize leave balances for a new user for the current year.
     * Called during registration (Business Rule 10).
     */
    @Transactional
    public void initializeBalancesForUser(User user) {
        int currentYear = LocalDate.now().getYear();

        if (leaveBalanceRepository.existsByUserIdAndYear(user.getId(), currentYear)) {
            return; // already initialized
        }

        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        for (LeaveType lt : leaveTypes) {
            LeaveBalance balance = LeaveBalance.builder()
                    .user(user)
                    .leaveType(lt)
                    .year(currentYear)
                    .totalDays(lt.getMaxDaysPerYear())
                    .usedDays(0)
                    .build();
            leaveBalanceRepository.save(balance);
        }
    }

    /**
     * Check if user has sufficient balance for requested leave days.
     */
    @Transactional(readOnly = true)
    public void checkSufficientBalance(Long userId, Integer leaveTypeId, int totalDays) {
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, currentYear)
                .orElseThrow(() -> new InsufficientBalanceException("No leave balance found for this leave type"));

        if (balance.getRemainingDays() < totalDays) {
            throw new InsufficientBalanceException("Insufficient leave balance. Available: "
                    + balance.getRemainingDays() + " days, Requested: " + totalDays + " days");
        }
    }

    /**
     * Deduct balance on APPROVAL (Business Rule 2).
     */
    @Transactional
    public void deductBalance(Long userId, Integer leaveTypeId, int totalDays) {
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, currentYear)
                .orElseThrow(() -> new InsufficientBalanceException("No leave balance found"));

        balance.setUsedDays(balance.getUsedDays() + totalDays);
        leaveBalanceRepository.save(balance);
    }

    /**
     * Restore balance on REJECTION or CANCELLATION of an approved leave (Business Rule 3).
     */
    @Transactional
    public void restoreBalance(Long userId, Integer leaveTypeId, int totalDays) {
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeIdAndYear(userId, leaveTypeId, currentYear)
                .orElse(null);

        if (balance != null) {
            balance.setUsedDays(Math.max(0, balance.getUsedDays() - totalDays));
            leaveBalanceRepository.save(balance);
        }
    }

    /**
     * Get leave balances for a user for a given year.
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalances(Long userId, int year) {
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdAndYear(userId, year);
        return balances.stream()
                .map(b -> LeaveBalanceResponse.builder()
                        .leaveTypeName(b.getLeaveType().getName())
                        .total(b.getTotalDays())
                        .used(b.getUsedDays())
                        .remaining(b.getRemainingDays())
                        .build())
                .collect(Collectors.toList());
    }
}
