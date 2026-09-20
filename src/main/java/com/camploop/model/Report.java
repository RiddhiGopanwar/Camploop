package com.camploop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A simple, unmoderated report on a listing. V1 just stores it — no moderation
 * workflow yet (that's a V2 admin-dashboard feature).
 */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Profile reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Report() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Profile getReporter() { return reporter; }
    public void setReporter(Profile reporter) { this.reporter = reporter; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
