package com.example.lms_backend.service;

import com.example.lms_backend.dto.LoginRequest;
import com.example.lms_backend.dto.LoginResponse;
import com.example.lms_backend.dto.RegisterRequest;
import com.example.lms_backend.entity.Role;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.repository.UserRepository;
import com.example.lms_backend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LeaveBalanceService leaveBalanceService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider,
                       LeaveBalanceService leaveBalanceService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.leaveBalanceService = leaveBalanceService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be EMPLOYEE, MANAGER, or ADMIN");
        }

        String employeeId = generateEmployeeId();

        User user = User.builder()
                .employeeId(employeeId)
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // Rule 10: Initialize leave balances for the new user
        leaveBalanceService.initializeBalancesForUser(savedUser);

        log.info("EMAIL_SIM → To: {} | Subject: Registration Successful | Body: Welcome to LMS, {}! Your employee ID is {}.",
                user.getEmail(), user.getFullName(), user.getEmployeeId());
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (!user.getIsActive()) {
                throw new BadCredentialsException("Account is deactivated");
            }

            String token = jwtTokenProvider.generateToken(
                    user.getEmail(), user.getRole().name(), user.getId());

            log.info("User logged in: {} ({})", user.getEmail(), user.getRole());

            return LoginResponse.builder()
                    .token(token)
                    .role(user.getRole().name())
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .employeeId(user.getEmployeeId())
                    .build();
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    private String generateEmployeeId() {
        long count = userRepository.count() + 1;
        String empId;
        do {
            empId = String.format("EMP%03d", count);
            count++;
        } while (userRepository.existsByEmployeeId(empId));
        return empId;
    }
}
