package com.example.lms_backend.service;

import com.example.lms_backend.dto.*;
import com.example.lms_backend.entity.*;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.exception.UnauthorizedActionException;
import com.example.lms_backend.repository.LeaveApplicationRepository;
import com.example.lms_backend.repository.LeaveTypeRepository;
import com.example.lms_backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LeaveApplicationRepository leaveApplicationRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveOverlapDetectionService overlapDetectionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserService userService;

    public LeaveService(LeaveApplicationRepository leaveApplicationRepository,
            LeaveTypeRepository leaveTypeRepository,
            UserRepository userRepository,
            LeaveBalanceService leaveBalanceService,
            LeaveOverlapDetectionService overlapDetectionService,
            AuditService auditService,
            NotificationService notificationService,
            UserService userService) {
        this.leaveApplicationRepository = leaveApplicationRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.overlapDetectionService = overlapDetectionService;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    // ==================== LEAVE TYPES ====================

    @Transactional(readOnly = true)
    public List<LeaveTypeDTO> getAllLeaveTypes() {
        return leaveTypeRepository.findAll().stream()
                .map(LeaveTypeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeaveTypeDTO createLeaveType(LeaveTypeDTO dto) {
        if (leaveTypeRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Leave type with name '" + dto.getName() + "' already exists");
        }
        LeaveType leaveType = LeaveType.builder()
                .name(dto.getName())
                .maxDaysPerYear(dto.getMaxDaysPerYear())
                .carryForward(dto.getCarryForward() != null ? dto.getCarryForward() : false)
                .description(dto.getDescription())
                .build();
        LeaveType saved = leaveTypeRepository.save(leaveType);
        return LeaveTypeDTO.fromEntity(saved);
    }

    // ==================== EMPLOYEE LEAVE ====================

    @Transactional
    public LeaveApplicationResponse applyLeave(LeaveApplicationRequest request) {
        User currentUser = userService.getCurrentUser();

        LocalDate startDate = LocalDate.parse(request.getStartDate(), DATE_FMT);
        LocalDate endDate = LocalDate.parse(request.getEndDate(), DATE_FMT);

        // Rule 7: Start date must not be in the past
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date must not be in the past");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }

        int totalDays = (int) (ChronoUnit.DAYS.between(startDate, endDate) + 1);

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));

        // Rule 1: Overlap check
        overlapDetectionService.checkOverlap(currentUser.getId(), startDate, endDate);

        // Check balance (but don't deduct yet — Rule 2: deduct only on approval)
        leaveBalanceService.checkSufficientBalance(currentUser.getId(), leaveType.getId(), totalDays);

        LeaveApplication application = LeaveApplication.builder()
                .applicant(currentUser)
                .leaveType(leaveType)
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(totalDays)
                .status(LeaveStatus.PENDING)
                .reason(request.getReason())
                .build();

        LeaveApplication saved = leaveApplicationRepository.save(application);

        // Rule 9: Audit log
        auditService.logAction(saved, currentUser, "APPLIED", null);

        // Rule 11: Notification
        if (currentUser.getManager() != null) {
            notificationService.sendLeaveAppliedNotification(
                    currentUser.getManager().getEmail(),
                    currentUser.getFullName(),
                    request.getStartDate(),
                    request.getEndDate());
        }

        return LeaveApplicationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<LeaveApplicationResponse> getMyLeaves(String status, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        Page<LeaveApplication> page;
        if (status == null || status.equalsIgnoreCase("ALL")) {
            page = leaveApplicationRepository.findByApplicantId(currentUser.getId(), pageable);
        } else {
            LeaveStatus leaveStatus = LeaveStatus.valueOf(status.toUpperCase());
            page = leaveApplicationRepository.findByApplicantIdAndStatus(
                    currentUser.getId(), leaveStatus, pageable);
        }

        return page.map(LeaveApplicationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public LeaveApplicationResponse getLeaveById(Long id) {
        User currentUser = userService.getCurrentUser();
        LeaveApplication la = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found"));

        // Employee can only see their own leaves
        if (!la.getApplicant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only view your own leave applications");
        }

        return LeaveApplicationResponse.fromEntity(la);
    }

    @Transactional
    public LeaveApplicationResponse cancelLeave(Long id) {
        User currentUser = userService.getCurrentUser();
        LeaveApplication la = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found"));

        if (!la.getApplicant().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException("You can only cancel your own leave applications");
        }

        // Rule 4: Only PENDING leaves can be cancelled
        if (la.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Only PENDING leaves can be cancelled");
        }

        la.setStatus(LeaveStatus.CANCELLED);
        LeaveApplication saved = leaveApplicationRepository.save(la);

        // Rule 9: Audit log
        auditService.logAction(saved, currentUser, "CANCELLED", null);

        // Rule 11: Notification
        if (currentUser.getManager() != null) {
            notificationService.sendLeaveCancelledNotification(
                    currentUser.getManager().getEmail(),
                    currentUser.getFullName(),
                    la.getStartDate().format(DATE_FMT),
                    la.getEndDate().format(DATE_FMT));
        }

        return LeaveApplicationResponse.fromEntity(saved);
    }

    // ==================== MANAGER LEAVE ====================

    @Transactional(readOnly = true)
    public Page<LeaveApplicationResponse> getPendingLeavesForManager(Pageable pageable) {
        User manager = userService.getCurrentUser();
        List<Long> teamIds = getTeamMemberIds(manager);

        if (teamIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return leaveApplicationRepository
                .findByApplicantIdsAndStatus(teamIds, LeaveStatus.PENDING, pageable)
                .map(LeaveApplicationResponse::fromEntity);
    }

    @Transactional
    public LeaveApplicationResponse approveLeave(Long id, String remarks) {
        User manager = userService.getCurrentUser();
        LeaveApplication la = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found"));

        // Rule 5: Manager scope — direct reports only
        validateManagerScope(manager, la);

        // Rule 6: Self-approval blocked
        if (la.getApplicant().getId().equals(manager.getId())) {
            throw new UnauthorizedActionException("You cannot approve your own leave");
        }

        if (la.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave is not in PENDING status");
        }

        la.setStatus(LeaveStatus.APPROVED);
        la.setApprovedBy(manager);
        la.setRemarks(remarks);
        LeaveApplication saved = leaveApplicationRepository.save(la);

        // Rule 2: Balance deduct on APPROVAL
        leaveBalanceService.deductBalance(
                la.getApplicant().getId(),
                la.getLeaveType().getId(),
                la.getTotalDays());

        // Rule 9: Audit log
        auditService.logAction(saved, manager, "APPROVED", remarks);

        // Rule 11: Notification
        notificationService.sendLeaveApprovedNotification(
                la.getApplicant().getEmail(),
                la.getApplicant().getFullName(),
                la.getStartDate().format(DATE_FMT),
                la.getEndDate().format(DATE_FMT),
                manager.getFullName());

        return LeaveApplicationResponse.fromEntity(saved);
    }

    @Transactional
    public LeaveApplicationResponse rejectLeave(Long id, String remarks) {
        User manager = userService.getCurrentUser();
        LeaveApplication la = leaveApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave application not found"));

        // Rule 5: Manager scope
        validateManagerScope(manager, la);

        if (la.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave is not in PENDING status");
        }

        // Rule 8: Remarks required for rejection
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Remarks are required when rejecting a leave");
        }

        la.setStatus(LeaveStatus.REJECTED);
        la.setApprovedBy(manager);
        la.setRemarks(remarks);
        LeaveApplication saved = leaveApplicationRepository.save(la);

        // Rule 9: Audit log
        auditService.logAction(saved, manager, "REJECTED", remarks);

        // Rule 11: Notification
        notificationService.sendLeaveRejectedNotification(
                la.getApplicant().getEmail(),
                la.getApplicant().getFullName(),
                la.getStartDate().format(DATE_FMT),
                la.getEndDate().format(DATE_FMT),
                manager.getFullName(),
                remarks);

        return LeaveApplicationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<LeaveApplicationResponse> getTeamLeaves(String status, Pageable pageable) {
        User manager = userService.getCurrentUser();
        List<Long> teamIds = getTeamMemberIds(manager);

        if (teamIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<LeaveApplication> page;
        if (status == null || status.equalsIgnoreCase("ALL")) {
            page = leaveApplicationRepository.findByApplicantIdIn(teamIds, pageable);
        } else {
            LeaveStatus leaveStatus = LeaveStatus.valueOf(status.toUpperCase());
            page = leaveApplicationRepository.findByApplicantIdInAndStatus(teamIds, leaveStatus, pageable);
        }

        return page.map(LeaveApplicationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public DashboardDTO getManagerDashboard() {
        User manager = userService.getCurrentUser();
        List<Long> teamIds = getTeamMemberIds(manager);

        if (teamIds.isEmpty()) {
            return DashboardDTO.builder()
                    .totalPending(0L)
                    .totalApproved(0L)
                    .totalRejected(0L)
                    .leaveByType(Collections.emptyMap())
                    .leaveByMonth(Collections.emptyMap())
                    .build();
        }

        // Status counts
        List<Object[]> statusCounts = leaveApplicationRepository.countByStatusForTeam(teamIds);
        long pending = 0, approved = 0, rejected = 0;
        for (Object[] row : statusCounts) {
            LeaveStatus s = (LeaveStatus) row[0];
            long count = (Long) row[1];
            switch (s) {
                case PENDING -> pending = count;
                case APPROVED -> approved = count;
                case REJECTED -> rejected = count;
                default -> {
                }
            }
        }

        // Leave by type
        List<Object[]> typeCounts = leaveApplicationRepository.countApprovedByTypeForTeam(teamIds);
        Map<String, Long> leaveByType = new LinkedHashMap<>();
        for (Object[] row : typeCounts) {
            leaveByType.put((String) row[0], (Long) row[1]);
        }

        // Leave by month
        List<Object[]> monthCounts = leaveApplicationRepository.countApprovedByMonthForTeam(teamIds);
        Map<String, Long> leaveByMonth = new LinkedHashMap<>();
        for (Object[] row : monthCounts) {
            leaveByMonth.put((String) row[0], (Long) row[1]);
        }

        return DashboardDTO.builder()
                .totalPending(pending)
                .totalApproved(approved)
                .totalRejected(rejected)
                .leaveByType(leaveByType)
                .leaveByMonth(leaveByMonth)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> getTeamCalendar(int month, int year) {
        User manager = userService.getCurrentUser();
        List<Long> teamIds = getTeamMemberIds(manager);

        if (teamIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<LeaveApplication> leaves = leaveApplicationRepository.findTeamCalendar(teamIds, month, year);

        return leaves.stream()
                .map(la -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("employeeName", la.getApplicant().getFullName());
                    entry.put("startDate", la.getStartDate().format(DATE_FMT));
                    entry.put("endDate", la.getEndDate().format(DATE_FMT));
                    entry.put("leaveType", la.getLeaveType().getName());
                    entry.put("status", la.getStatus().name());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    // ==================== HELPERS ====================

    private List<Long> getTeamMemberIds(User manager) {
        return userRepository.findByManagerId(manager.getId())
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private void validateManagerScope(User manager, LeaveApplication la) {
        if (la.getApplicant().getManager() == null ||
                !la.getApplicant().getManager().getId().equals(manager.getId())) {
            throw new UnauthorizedActionException("You can only approve leaves of your direct team members");
        }
    }
}
