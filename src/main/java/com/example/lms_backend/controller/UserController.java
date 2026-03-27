package com.example.lms_backend.controller;

import com.example.lms_backend.dto.ApiResponse;
import com.example.lms_backend.dto.UserDTO;
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
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

        private final UserService userService;

        public UserController(UserService userService) {
                this.userService = userService;
        }

        @GetMapping("/me")
        @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
        @Operation(summary = "Get current user profile", description = "Returns the profile of the currently authenticated user")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile retrieved")
        })
        public ResponseEntity<ApiResponse<UserDTO>> getMe() {
                UserDTO user = userService.getMe();
                return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", user));
        }

        @PutMapping("/me")
        @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN')")
        @Operation(summary = "Update current user profile", description = "Updates the full name of the currently authenticated user")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated")
        })
        public ResponseEntity<ApiResponse<UserDTO>> updateMe(@RequestBody Map<String, String> body) {
                String fullName = body.get("fullName");
                UserDTO updated = userService.updateMe(fullName);
                return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
        }

        @GetMapping("/team")
        @PreAuthorize("hasRole('MANAGER')")
        @Operation(summary = "Get team members", description = "Returns direct reports of the current manager")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Team members retrieved")
        })
        public ResponseEntity<ApiResponse<List<UserDTO>>> getTeamMembers() {
                List<UserDTO> team = userService.getTeamMembers();
                return ResponseEntity.ok(ApiResponse.success("Team members retrieved successfully", team));
        }

        @GetMapping
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Get all users (paginated)", description = "Admin only — returns a paginated list of all users")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved")
        })
        public ResponseEntity<ApiResponse<Page<UserDTO>>> getAllUsers(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                Pageable pageable = PageRequest.of(page, size);
                Page<UserDTO> users = userService.getAllUsers(pageable);
                return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
        @Operation(summary = "Get user by ID", description = "Admin can view any user; Manager can only view direct reports")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
                UserDTO user = userService.getUserById(id);
                return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
        }

        @PutMapping("/{id}/assign-manager")
        @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
        @Operation(summary = "Assign manager to an employee", description = "Links an employee to a manager. Required to enable manager visibility of leave requests.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Manager assigned successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<ApiResponse<UserDTO>> assignManager(
                        @PathVariable Long id,
                        @RequestParam Long managerId) {
                UserDTO updated = userService.assignManager(id, managerId);
                return ResponseEntity.ok(ApiResponse.success("Manager assigned successfully", updated));
        }
}
