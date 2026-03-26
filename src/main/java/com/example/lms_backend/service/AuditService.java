package com.example.lms_backend.service;

import com.example.lms_backend.dto.AuditLogDTO;
import com.example.lms_backend.entity.AuditLog;
import com.example.lms_backend.entity.LeaveApplication;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(LeaveApplication leaveApplication, User actionBy, String action, String remarks) {
        AuditLog auditLog = AuditLog.builder()
                .leaveApplication(leaveApplication)
                .actionBy(actionBy)
                .action(action)
                .remarks(remarks)
                .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsForLeave(Long leaveApplicationId) {
        return auditLogRepository.findByLeaveApplicationIdOrderByActionAtDesc(leaveApplicationId)
                .stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
