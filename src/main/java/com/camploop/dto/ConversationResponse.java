package com.camploop.dto;

import com.camploop.model.Conversation;
import com.camploop.model.Message;
import com.camploop.model.Profile;

import java.time.LocalDateTime;
import java.util.UUID;

public class ConversationResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productStatus;
    private UUID buyerId;
    private UUID sellerId;
    private UUID otherUserId;
    private String otherUserName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    public ConversationResponse(Conversation c, Profile me, Message last) {
        this.id = c.getId();
        this.productId = c.getProduct().getId();
        this.productName = c.getProduct().getName();
        this.productStatus = c.getProduct().getStatus().name();
        this.buyerId = c.getBuyer().getId();
        this.sellerId = c.getSeller().getId();
        Profile other = c.otherParty(me);
        this.otherUserId = other.getId();
        this.otherUserName = other.getName();
        if (last != null) {
            this.lastMessage = last.getBody();
            this.lastMessageAt = last.getCreatedAt();
        }
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductStatus() { return productStatus; }
    public UUID getBuyerId() { return buyerId; }
    public UUID getSellerId() { return sellerId; }
    public UUID getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getLastMessage() { return lastMessage; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
}
