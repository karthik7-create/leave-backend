package com.example.lms_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendLeaveAppliedNotification(String managerEmail, String employeeName, String startDate, String endDate) {
        log.info("EMAIL_SIM → To: {} | Subject: New Leave Request | Body: {} has applied for leave from {} to {}. Please review.",
                managerEmail, employeeName, startDate, endDate);
    }

    public void sendLeaveApprovedNotification(String employeeEmail, String employeeName, String startDate, String endDate, String approverName) {
        log.info("EMAIL_SIM → To: {} | Subject: Leave Approved | Body: Hi {}, your leave from {} to {} has been approved by {}.",
                employeeEmail, employeeName, startDate, endDate, approverName);
    }

    public void sendLeaveRejectedNotification(String employeeEmail, String employeeName, String startDate, String endDate, String approverName, String remarks) {
        log.info("EMAIL_SIM → To: {} | Subject: Leave Rejected | Body: Hi {}, your leave from {} to {} has been rejected by {}. Reason: {}",
                employeeEmail, employeeName, startDate, endDate, approverName, remarks);
    }

    public void sendLeaveCancelledNotification(String managerEmail, String employeeName, String startDate, String endDate) {
        log.info("EMAIL_SIM → To: {} | Subject: Leave Cancelled | Body: {} has cancelled their leave from {} to {}.",
                managerEmail, employeeName, startDate, endDate);
    }
}
