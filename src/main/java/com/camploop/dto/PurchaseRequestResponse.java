package com.camploop.dto;

import com.camploop.model.Product;
import com.camploop.model.PurchaseRequest;
import com.camploop.model.enums.ListingStatus;
import com.camploop.model.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PurchaseRequestResponse {
    private Long id;
    private RequestStatus status;
    private LocalDateTime createdAt;

    private Long productId;
    private String productName;
    private String productCategory;
    private String productImage;
    private BigDecimal productPrice;
    private ListingStatus productStatus;

    private UUID buyerId;
    private String buyerName;
    private String buyerCollege;
    private UUID sellerId;
    private String sellerName;

    public PurchaseRequestResponse(PurchaseRequest r) {
        this.id = r.getId();
        this.status = r.getStatus();
        this.createdAt = r.getCreatedAt();

        Product p = r.getProduct();
        this.productId = p.getId();
        this.productName = p.getName();
        this.productCategory = p.getCategory() != null ? p.getCategory().name() : null;
        this.productImage = p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl();
        this.productPrice = p.getSellingPrice();
        this.productStatus = p.getStatus();

        this.buyerId = r.getBuyer().getId();
        this.buyerName = r.getBuyer().getName();
        this.buyerCollege = r.getBuyer().getCollege();
        this.sellerId = r.getSeller().getId();
        this.sellerName = r.getSeller().getName();
    }

    public Long getId() { return id; }
    public RequestStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getProductCategory() { return productCategory; }
    public String getProductImage() { return productImage; }
    public BigDecimal getProductPrice() { return productPrice; }
    public ListingStatus getProductStatus() { return productStatus; }
    public UUID getBuyerId() { return buyerId; }
    public String getBuyerName() { return buyerName; }
    public String getBuyerCollege() { return buyerCollege; }
    public UUID getSellerId() { return sellerId; }
    public String getSellerName() { return sellerName; }
}
