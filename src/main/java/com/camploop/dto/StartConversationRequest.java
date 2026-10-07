package com.camploop.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class StartConversationRequest {

    @NotNull(message = "Product is required")
    private Long productId;

    // Only used when the SELLER opens the chat (to pick which interested buyer).
    // A buyer never needs to send this — they are always themselves.
    private UUID buyerId;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public UUID getBuyerId() { return buyerId; }
    public void setBuyerId(UUID buyerId) { this.buyerId = buyerId; }
}
