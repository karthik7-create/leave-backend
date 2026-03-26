package com.example.lms_backend.exception;

public class LeaveOverlapException extends RuntimeException {
    public LeaveOverlapException(String message) {
        super(message);
    }
}
