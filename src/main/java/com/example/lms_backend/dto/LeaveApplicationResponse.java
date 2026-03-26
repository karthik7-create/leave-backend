package com.example.lms_backend.dto;

import com.example.lms_backend.entity.LeaveApplication;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApplicationResponse {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Long id;
    private String applicantName;
    private String applicantEmployeeId;
    private String leaveType;
    private String startDate;
    private String endDate;
    private Integer totalDays;
    private String status;
    private String reason;
    private String appliedAt;
    private String approvedByName;
    private String remarks;

    public static LeaveApplicationResponse fromEntity(LeaveApplication la) {
        return LeaveApplicationResponse.builder()
                .id(la.getId())
                .applicantName(la.getApplicant().getFullName())
                .applicantEmployeeId(la.getApplicant().getEmployeeId())
                .leaveType(la.getLeaveType().getName())
                .startDate(la.getStartDate().format(DATE_FMT))
                .endDate(la.getEndDate().format(DATE_FMT))
                .totalDays(la.getTotalDays())
                .status(la.getStatus().name())
                .reason(la.getReason())
                .appliedAt(la.getAppliedAt() != null ? la.getAppliedAt().format(TS_FMT) : null)
                .approvedByName(la.getApprovedBy() != null ? la.getApprovedBy().getFullName() : null)
                .remarks(la.getRemarks())
                .build();
    }
}
