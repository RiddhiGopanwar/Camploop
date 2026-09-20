package com.camploop.model;

import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.enums.ListingStatus;
import com.camploop.model.enums.ListingType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Profile seller;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    // What the seller originally paid / MRP — optional, used only to show savings
    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    // What the buyer pays now — nullable because listing_type can be EXCHANGE or DONATE
    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Condition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 20)
    private ListingType listingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.AVAILABLE;

    // Optional on-campus meeting point: Hostel / Department / Library / Main Gate / Other
    @Column(name = "pickup_location", length = 150)
    private String pickupLocation;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public Product() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ListingStatus.AVAILABLE;
        }
    }

    // ----- Getters & Setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Profile getSeller() { return seller; }
    public void setSeller(Profile seller) { this.seller = seller; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Condition getCondition() { return condition; }
    public void setCondition(Condition condition) { this.condition = condition; }
    public ListingType getListingType() { return listingType; }
    public void setListingType(ListingType listingType) { this.listingType = listingType; }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }
}
