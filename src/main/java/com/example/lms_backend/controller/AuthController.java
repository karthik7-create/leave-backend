package com.example.lms_backend.controller;

import com.example.lms_backend.dto.ApiResponse;
import com.example.lms_backend.dto.LoginRequest;
import com.example.lms_backend.dto.LoginResponse;
import com.example.lms_backend.dto.RegisterRequest;
import com.example.lms_backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Public authentication endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new employee",
            description = "Creates a new EMPLOYEE account. Role is always EMPLOYEE regardless of input.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Registration successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error or email already exists")
    })
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully"));
    }

    @PostMapping("/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Create user with any role",
            description = "Admin-only endpoint to create MANAGER or ADMIN accounts. Role field is respected.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "User created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Validation error or email already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Only ADMIN can access this endpoint")
    })
    public ResponseEntity<ApiResponse<Void>> createUser(@Valid @RequestBody RegisterRequest request) {
        authService.registerWithRole(request);
        return ResponseEntity.ok(ApiResponse.success("User created successfully with role: " + request.getRole()));
    }

    @PostMapping("/login")
    @Operation(summary = "User login",
            description = "Authenticates user and returns JWT token with user details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Invalid credentials")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout",
            description = "Logs out the user (client should discard the token)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Logout successful")
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}

