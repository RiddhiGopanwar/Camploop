package com.camploop.dto;

import com.camploop.model.Wishlist;

public class WishlistResponse {
    private Long id;
    private ProductResponse product;

    public WishlistResponse(Wishlist w) {
        this.id = w.getId();
        this.product = new ProductResponse(w.getProduct());
    }

    public Long getId() { return id; }
    public ProductResponse getProduct() { return product; }
}
