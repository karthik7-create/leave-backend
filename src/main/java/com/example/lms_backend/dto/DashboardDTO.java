package com.example.lms_backend.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {

    private Long totalPending;
    private Long totalApproved;
    private Long totalRejected;
    private Map<String, Long> leaveByType;
    private Map<String, Long> leaveByMonth;
}
