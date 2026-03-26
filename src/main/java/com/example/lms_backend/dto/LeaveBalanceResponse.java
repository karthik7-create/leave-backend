package com.example.lms_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceResponse {

    private String leaveTypeName;
    private Integer total;
    private Integer used;
    private Integer remaining;
}
