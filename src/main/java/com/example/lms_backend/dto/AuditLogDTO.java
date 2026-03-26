package com.example.lms_backend.dto;

import com.example.lms_backend.entity.AuditLog;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogDTO {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Long id;
    private Long leaveApplicationId;
    private String actionBy;
    private String action;
    private String remarks;
    private String actionAt;

    public static AuditLogDTO fromEntity(AuditLog log) {
        return AuditLogDTO.builder()
                .id(log.getId())
                .leaveApplicationId(log.getLeaveApplication().getId())
                .actionBy(log.getActionBy().getFullName())
                .action(log.getAction())
                .remarks(log.getRemarks())
                .actionAt(log.getActionAt() != null ? log.getActionAt().format(TS_FMT) : null)
                .build();
    }
}
