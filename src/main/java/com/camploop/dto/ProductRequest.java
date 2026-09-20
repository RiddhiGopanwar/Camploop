package com.camploop.dto;

import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.enums.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    // What the seller originally paid — optional, purely informational (shows savings)
    private BigDecimal originalPrice;

    // What the buyer pays now — required for SELL, ignored for EXCHANGE/DONATE
    private BigDecimal sellingPrice;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Condition is required")
    private Condition condition;

    @NotNull(message = "Listing type is required")
    private ListingType listingType;

    // Optional on-campus meeting point, e.g. "Hostel", "Library", "Main Gate"
    private String pickupLocation;

    // Public URLs already uploaded to Supabase Storage by the frontend, in display order
    private List<String> images;

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
    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
}
