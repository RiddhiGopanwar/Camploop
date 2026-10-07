package com.camploop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotBlank(message = "Message can't be empty")
    @Size(max = 1000, message = "Message is too long (max 1000 characters)")
    private String body;

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
