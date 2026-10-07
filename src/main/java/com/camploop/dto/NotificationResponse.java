package com.camploop.dto;

import com.camploop.model.Notification;
import com.camploop.model.enums.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String message;
    private Long productId;
    private Long referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.type = n.getType();
        this.message = n.getMessage();
        this.productId = n.getProduct() != null ? n.getProduct().getId() : null;
        this.referenceId = n.getReferenceId();
        this.read = n.isSeen();
        this.createdAt = n.getCreatedAt();
    }

    public Long getId() { return id; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public Long getProductId() { return productId; }
    public Long getReferenceId() { return referenceId; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
