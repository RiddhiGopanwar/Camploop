package com.camploop.dto;

import com.camploop.model.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {
    private Long id;
    private UUID senderId;
    private String body;
    private LocalDateTime createdAt;

    public MessageResponse(Message m) {
        this.id = m.getId();
        this.senderId = m.getSender().getId();
        this.body = m.getBody();
        this.createdAt = m.getCreatedAt();
    }

    public Long getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
