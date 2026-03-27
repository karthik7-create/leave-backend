package com.example.lms_backend.service;

import com.example.lms_backend.dto.UserDTO;
import com.example.lms_backend.entity.Role;
import com.example.lms_backend.entity.User;
import com.example.lms_backend.exception.ResourceNotFoundException;
import com.example.lms_backend.exception.UnauthorizedActionException;
import com.example.lms_backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the currently authenticated user.
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * GET /api/users/me
     */
    @Transactional(readOnly = true)
    public UserDTO getMe() {
        return UserDTO.fromEntity(getCurrentUser());
    }

    /**
     * PUT /api/users/me — update profile (only fullName allowed)
     */
    @Transactional
    public UserDTO updateMe(String fullName) {
        User user = getCurrentUser();
        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        User saved = userRepository.save(user);
        return UserDTO.fromEntity(saved);
    }

    /**
     * GET /api/users/team — Manager only: get direct reports
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getTeamMembers() {
        User manager = getCurrentUser();
        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedActionException("Only managers can view team members");
        }
        List<User> team = userRepository.findByManagerId(manager.getId());
        return team.stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/users — Admin only: paginated list of all users
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserDTO::fromEntity);
    }

    /**
     * GET /api/users/{id} — Admin or Manager
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Admin can see anyone; Manager can only see their direct reports
        if (currentUser.getRole() == Role.ADMIN) {
            return UserDTO.fromEntity(targetUser);
        }

        if (currentUser.getRole() == Role.MANAGER) {
            if (targetUser.getManager() != null &&
                    targetUser.getManager().getId().equals(currentUser.getId())) {
                return UserDTO.fromEntity(targetUser);
            }
            throw new UnauthorizedActionException("You can only view your direct team members");
        }

        throw new UnauthorizedActionException("Access denied");
    }

    /**
     * Find user by ID (internal use).
     */
    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * PUT /api/users/{id}/assign-manager — Assign a manager to an employee
     */
    @Transactional
    public UserDTO assignManager(Long employeeId, Long managerId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with id: " + managerId));

        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Target user (id=" + managerId + ") does not have MANAGER or ADMIN role");
        }

        if (employee.getId().equals(manager.getId())) {
            throw new IllegalArgumentException("A user cannot be their own manager");
        }

        employee.setManager(manager);
        User saved = userRepository.save(employee);
        return UserDTO.fromEntity(saved);
    }

    /**
     * Manager adds a team member by employee ID (e.g. "EMP003").
     */
    @Transactional
    public UserDTO addTeamMember(String employeeId) {
        User manager = getCurrentUser();

        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedActionException("Only managers can add team members");
        }

        User employee = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee found with ID: " + employeeId));

        // Validation 1: Can't add yourself
        if (employee.getId().equals(manager.getId())) {
            throw new IllegalArgumentException("You cannot add yourself to your own team");
        }

        // Validation 2: Must be an EMPLOYEE
        if (employee.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Only employees can be added to a team. " + employeeId + " has role: " + employee.getRole());
        }

        // Validation 3: Must not already belong to another manager
        if (employee.getManager() != null && !employee.getManager().getId().equals(manager.getId())) {
            throw new IllegalArgumentException(employeeId + " is already assigned to another manager");
        }

        // Validation 4: Already in this team
        if (employee.getManager() != null && employee.getManager().getId().equals(manager.getId())) {
            throw new IllegalArgumentException(employeeId + " is already in your team");
        }

        employee.setManager(manager);
        User saved = userRepository.save(employee);
        return UserDTO.fromEntity(saved);
    }

    /**
     * Manager removes a team member by employee ID.
     */
    @Transactional
    public void removeTeamMember(String employeeId) {
        User manager = getCurrentUser();

        if (manager.getRole() != Role.MANAGER) {
            throw new UnauthorizedActionException("Only managers can remove team members");
        }

        User employee = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No employee found with ID: " + employeeId));

        // Must be in THIS manager's team
        if (employee.getManager() == null || !employee.getManager().getId().equals(manager.getId())) {
            throw new IllegalArgumentException(employeeId + " is not in your team");
        }

        employee.setManager(null);
        userRepository.save(employee);
    }
}
