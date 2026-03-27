package com.example.lms_backend.controller;

import com.example.lms_backend.dto.*;
import com.example.lms_backend.service.LeaveService;
import com.example.lms_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
@Tag(name = "Manager Operations", description = "Manager leave management endpoints")
public class ManagerController {

    private final LeaveService leaveService;
    private final UserService userService;

    public ManagerController(LeaveService leaveService, UserService userService) {
        this.leaveService = leaveService;
        this.userService = userService;
    }

    @GetMapping("/leaves/pending")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get pending leaves for team")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Pending leaves retrieved")
    })
    public ResponseEntity<ApiResponse<Page<LeaveApplicationResponse>>> getPendingLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveApplicationResponse> leaves = leaveService.getPendingLeavesForManager(pageable);
        return ResponseEntity.ok(ApiResponse.success("Pending leaves retrieved successfully", leaves));
    }

    @PutMapping("/leaves/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve a leave application")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave approved")
    })
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> approveLeave(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequest request) {
        String remarks = (request != null) ? request.getRemarks() : null;
        LeaveApplicationResponse response = leaveService.approveLeave(id, remarks);
        return ResponseEntity.ok(ApiResponse.success("Leave approved successfully", response));
    }

    @PutMapping("/leaves/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Reject a leave application", description = "Remarks are required")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Leave rejected")
    })
    public ResponseEntity<ApiResponse<LeaveApplicationResponse>> rejectLeave(
            @PathVariable Long id,
            @RequestBody ApprovalRequest request) {
        LeaveApplicationResponse response = leaveService.rejectLeave(id, request.getRemarks());
        return ResponseEntity.ok(ApiResponse.success("Leave rejected successfully", response));
    }

    @GetMapping("/leaves/team")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get team leave history", description = "Paginated with status filter")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Team leaves retrieved")
    })
    public ResponseEntity<ApiResponse<Page<LeaveApplicationResponse>>> getTeamLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LeaveApplicationResponse> leaves = leaveService.getTeamLeaves(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Team leaves retrieved successfully", leaves));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get manager dashboard analytics")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Dashboard data retrieved")
    })
    public ResponseEntity<ApiResponse<DashboardDTO>> getDashboard() {
        DashboardDTO dashboard = leaveService.getManagerDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", dashboard));
    }

    @GetMapping("/leaves/calendar")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get team leave calendar", description = "Filter by month and year")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Calendar data retrieved")
    })
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getTeamCalendar(
            @RequestParam int month,
            @RequestParam int year) {
        List<Map<String, String>> calendar = leaveService.getTeamCalendar(month, year);
        return ResponseEntity.ok(ApiResponse.success("Team calendar retrieved successfully", calendar));
    }

    @GetMapping("/team")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Get team members")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Team members retrieved")
    })
    public ResponseEntity<ApiResponse<List<UserDTO>>> getTeamMembers() {
        List<UserDTO> team = userService.getTeamMembers();
        return ResponseEntity.ok(ApiResponse.success("Team members retrieved successfully", team));
    }

    @PutMapping("/add-member")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Add employee to team",
            description = "Manager adds an employee to their team using the employee's ID (e.g. EMP003)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Employee added to team"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Employee not found")
    })
    public ResponseEntity<ApiResponse<UserDTO>> addTeamMember(@RequestParam String employeeId) {
        UserDTO added = userService.addTeamMember(employeeId.trim().toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Employee " + employeeId + " added to your team successfully", added));
    }

    @PutMapping("/remove-member")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Remove employee from team",
            description = "Manager removes an employee from their team using the employee's ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Employee removed from team"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Employee not found")
    })
    public ResponseEntity<ApiResponse<Void>> removeTeamMember(@RequestParam String employeeId) {
        userService.removeTeamMember(employeeId.trim().toUpperCase());
        return ResponseEntity.ok(ApiResponse.success("Employee " + employeeId + " removed from your team"));
    }
}
