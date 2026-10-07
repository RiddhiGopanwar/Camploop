package com.camploop.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A private chat about ONE product between ONE buyer and the product's seller. */
@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "buyer_id"})
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Profile buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Profile seller;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Conversation() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public boolean hasParticipant(Profile p) {
        return p != null && (buyer.getId().equals(p.getId()) || seller.getId().equals(p.getId()));
    }

    public Profile otherParty(Profile me) {
        return buyer.getId().equals(me.getId()) ? seller : buyer;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Profile getBuyer() { return buyer; }
    public void setBuyer(Profile buyer) { this.buyer = buyer; }
    public Profile getSeller() { return seller; }
    public void setSeller(Profile seller) { this.seller = seller; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
