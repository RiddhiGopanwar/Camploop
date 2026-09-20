package com.camploop.dto;

import com.camploop.model.Product;
import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.enums.ListingStatus;
import com.camploop.model.enums.ListingType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;
    private BigDecimal savingsAmount;
    private Integer savingsPercent;
    private Category category;
    private String categoryLabel;
    private Condition condition;
    private String conditionLabel;
    private ListingType listingType;
    private ListingStatus status;
    private String pickupLocation;
    private LocalDateTime createdAt;
    private List<String> images;

    private UUID sellerId;
    private String sellerName;
    private String sellerCollege;

    public ProductResponse(Product p) {
        this.id = p.getId();
        this.name = p.getName();
        this.description = p.getDescription();
        this.originalPrice = p.getOriginalPrice();
        this.sellingPrice = p.getSellingPrice();
        this.category = p.getCategory();
        this.categoryLabel = p.getCategory() != null
                ? p.getCategory().getDisplayName()
                : null;
        this.condition = p.getCondition();
        this.conditionLabel = p.getCondition() != null
                ? p.getCondition().getDisplayName()
                : null;
        this.listingType = p.getListingType();
        this.status = p.getStatus();
        this.pickupLocation = p.getPickupLocation();
        this.createdAt = p.getCreatedAt();

        this.images = p.getImages().stream()
                .map(img -> img.getImageUrl())
                .collect(Collectors.toList());

        this.sellerId = p.getSeller() != null
                ? p.getSeller().getId()
                : null;
        this.sellerName = p.getSeller() != null
                ? p.getSeller().getName()
                : null;
        this.sellerCollege = p.getSeller() != null
                ? p.getSeller().getCollege()
                : null;

        if (p.getOriginalPrice() != null
                && p.getSellingPrice() != null
                && p.getOriginalPrice().compareTo(p.getSellingPrice()) > 0) {

            this.savingsAmount = p.getOriginalPrice()
                    .subtract(p.getSellingPrice());

            this.savingsPercent = savingsAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            p.getOriginalPrice(),
                            0,
                            RoundingMode.HALF_UP
                    )
                    .intValue();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public BigDecimal getSavingsAmount() {
        return savingsAmount;
    }

    public Integer getSavingsPercent() {
        return savingsPercent;
    }

    public Category getCategory() {
        return category;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public Condition getCondition() {
        return condition;
    }

    public String getConditionLabel() {
        return conditionLabel;
    }

    public ListingType getListingType() {
        return listingType;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<String> getImages() {
        return images;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getSellerCollege() {
        return sellerCollege;
    }
}