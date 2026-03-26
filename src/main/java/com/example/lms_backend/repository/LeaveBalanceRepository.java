package com.example.lms_backend.repository;

import com.example.lms_backend.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);

    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(Long userId, Integer leaveTypeId, Integer year);

    boolean existsByUserIdAndYear(Long userId, Integer year);
}
