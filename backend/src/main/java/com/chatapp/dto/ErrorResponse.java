package com.chatapp.dto;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API errors
 * Provides consistent error information across all endpoints
 */
public class ErrorResponse {

    private int status;
    private String message;
    private String detail;
    private LocalDateTime timestamp;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String message, String detail) {
        this.status = status;
        this.message = message;
        this.detail = detail;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
