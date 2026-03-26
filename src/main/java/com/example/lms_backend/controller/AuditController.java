package com.example.lms_backend.controller;

import com.example.lms_backend.dto.ApiResponse;
import com.example.lms_backend.dto.AuditLogDTO;
import com.example.lms_backend.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Audit log endpoints")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/leaves/{leaveId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get audit logs for a leave application",
            description = "Returns the full history of status changes for a leave")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Audit logs retrieved")
    })
    public ResponseEntity<ApiResponse<List<AuditLogDTO>>> getAuditLogs(@PathVariable Long leaveId) {
        List<AuditLogDTO> logs = auditService.getAuditLogsForLeave(leaveId);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
    }
}
