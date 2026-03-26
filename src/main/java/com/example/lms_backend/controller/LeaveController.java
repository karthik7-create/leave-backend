package com.example.lms_backend.controller;

import com.example.lms_backend.dto.*;
import com.example.lms_backend.service.LeaveBalanceService;
import com.example.lms_backend.service.LeaveService;
import com.example.lms_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Leave Management", description = "Employee leave operations")
public class LeaveController {

    private final LeaveService leaveService;
    private final LeaveBalanceService leaveBalanceService;
    private final UserService userService;

    public LeaveController(LeaveService leaveService,
                           LeaveBalanceService leaveBalanceService,
                           UserService userService) {
        this.leaveService = leaveService;
        this.leaveBalanceService = leaveBalanceService;
        this.userService = userService;
    }

    // ==================== LEAVE TYPES ====================

    @GetMapping("/leave-types")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
    @Operation(summary = "Get all leave types")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave types retrieved")
    })
    public ResponseEntity<ApiResponse<List<LeaveTypeDTO>>> getLeaveTypes() {
        List<LeaveTypeDTO> types = leaveService.getAllLeaveTypes();
        return ResponseEntity.ok(ApiResponse.success("Leave types retrieved successfully", types));
    }

    @PostMapping("/leave-types")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new leave type", description = "Admin only")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave type created")
    })
    public ResponseEntity<ApiResponse<LeaveTypeDTO>> createLeaveType(
            @Valid @RequestBody LeaveTypeDTO request) {
        LeaveTypeDTO created = leaveService.createLeaveType(request);
        return ResponseEntity.ok(ApiResponse.success("Leave type created successfully", created));
    }

    // ==================== EMPLOYEE LEAVE ====================

    @PostMapping("/leaves")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    @Operation(summary = "Apply for leave", description = "Submit a new leave application")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave applied successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error")
    })
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> applyLeave(
            @Valid @RequestBody LeaveApplicationRequest request) {
        LeaveApplicationResponse response = leaveService.applyLeave(request);
        return ResponseEntity.ok(ApiResponse.success("Leave applied successfully", response));
    }

    @GetMapping("/leaves/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    @Operation(summary = "Get my leaves", description = "Paginated list of current user's leaves")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leaves retrieved")
    })
    public ResponseEntity<ApiResponse<Page<LeaveApplicationResponse>>> getMyLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveApplicationResponse> leaves = leaveService.getMyLeaves(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Leaves retrieved successfully", leaves));
    }

    @GetMapping("/leaves/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    @Operation(summary = "Get leave by ID", description = "Employee can only see own leaves")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Leave not found")
    })
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> getLeaveById(@PathVariable Long id) {
        LeaveApplicationResponse response = leaveService.getLeaveById(id);
        return ResponseEntity.ok(ApiResponse.success("Leave retrieved successfully", response));
    }

    @PutMapping("/leaves/{id}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    @Operation(summary = "Cancel leave", description = "Only PENDING leaves can be cancelled")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave cancelled")
    })
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> cancelLeave(@PathVariable Long id) {
        LeaveApplicationResponse response = leaveService.cancelLeave(id);
        return ResponseEntity.ok(ApiResponse.success("Leave cancelled successfully", response));
    }

    @GetMapping("/leaves/balance")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER')")
    @Operation(summary = "Get leave balance", description = "Returns balances for all leave types")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Balance retrieved")
    })
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> getLeaveBalance(
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        Long userId = userService.getCurrentUser().getId();
        List<LeaveBalanceResponse> balances = leaveBalanceService.getBalances(userId, targetYear);
        return ResponseEntity.ok(ApiResponse.success("Leave balance retrieved successfully", balances));
    }
}
