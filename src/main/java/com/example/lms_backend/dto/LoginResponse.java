package com.example.lms_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String role;
    private Long userId;
    private String fullName;
    private String employeeId;
}
