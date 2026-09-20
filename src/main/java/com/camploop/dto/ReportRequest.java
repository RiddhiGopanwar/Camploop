package com.camploop.dto;

import jakarta.validation.constraints.NotBlank;

public class ReportRequest {
    @NotBlank(message = "Please describe why you're reporting this listing")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
