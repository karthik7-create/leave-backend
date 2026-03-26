package com.example.lms_backend.repository;

import com.example.lms_backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByLeaveApplicationIdOrderByActionAtDesc(Long leaveApplicationId);
}
