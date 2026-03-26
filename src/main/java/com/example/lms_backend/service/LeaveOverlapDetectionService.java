package com.example.lms_backend.service;

import com.example.lms_backend.entity.LeaveApplication;
import com.example.lms_backend.entity.LeaveStatus;
import com.example.lms_backend.exception.LeaveOverlapException;
import com.example.lms_backend.repository.LeaveApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveOverlapDetectionService {

    private final LeaveApplicationRepository leaveApplicationRepository;

    public LeaveOverlapDetectionService(LeaveApplicationRepository leaveApplicationRepository) {
        this.leaveApplicationRepository = leaveApplicationRepository;
    }

    public void checkOverlap(Long userId, LocalDate startDate, LocalDate endDate) {
        List<LeaveStatus> activeStatuses = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

        List<LeaveApplication> overlapping = leaveApplicationRepository.findOverlappingLeaves(
                userId, startDate, endDate, activeStatuses);

        if (!overlapping.isEmpty()) {
            throw new LeaveOverlapException("Leave dates overlap with existing leave");
        }
    }
}
