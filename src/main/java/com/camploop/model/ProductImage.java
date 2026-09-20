package com.camploop.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Public URL of the file in the Supabase Storage "product-images" bucket
    @Column(name = "image_url", nullable = false, length = 600)
    private String imageUrl;

    // Preserves the order the seller uploaded/arranged images in
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public ProductImage() {
    }

    public ProductImage(Product product, String imageUrl, Integer sortOrder) {
        this.product = product;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
