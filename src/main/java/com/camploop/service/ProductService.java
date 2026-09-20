package com.camploop.service;

import com.camploop.dto.ProductRequest;
import com.camploop.exception.ApiException;
import com.camploop.model.Product;
import com.camploop.model.ProductImage;
import com.camploop.model.Profile;
import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.enums.ListingStatus;
import com.camploop.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(ProductRequest request, Profile seller) {
        Product product = new Product();
        applyRequest(product, request);
        product.setSeller(seller);
        return productRepository.save(product);
    }

    public Product update(Long productId, ProductRequest request, Profile currentUser) {
        Product product = getById(productId);
        assertOwner(product, currentUser);
        applyRequest(product, request);
        return productRepository.save(product);
    }

    public void delete(Long productId, Profile currentUser) {
        Product product = getById(productId);
        assertOwner(product, currentUser);
        productRepository.delete(product);
    }

    /** Marks a listing SOLD or UNAVAILABLE. Either way it drops out of active search
     *  results (see ProductRepository.search, which only matches status = AVAILABLE)
     *  while the row itself — and its place in the seller's history — is kept. */
    public Product markStatus(Long productId, ListingStatus status, Profile currentUser) {
        Product product = getById(productId);
        assertOwner(product, currentUser);
        product.setStatus(status);
        return productRepository.save(product);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));
    }

    public List<Product> getRecent() {
        return productRepository.findTop8ByStatusOrderByCreatedAtDesc(ListingStatus.AVAILABLE);
    }

    public List<Product> search(String keyword, Category category, Condition condition,
                                 BigDecimal minPrice, BigDecimal maxPrice) {
        return productRepository.search(ListingStatus.AVAILABLE, blankToNull(keyword), category, condition, minPrice, maxPrice);
    }

    public List<Product> getBySeller(Profile seller) {
        return productRepository.findBySeller(seller);
    }

    private void assertOwner(Product product, Profile currentUser) {
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new ApiException("You can only manage your own listings", HttpStatus.FORBIDDEN);
        }
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        // Exchange/Donate listings don't carry a selling price even if the form sent one
        product.setSellingPrice(request.getListingType() != null && request.getListingType().name().equals("SELL")
                ? request.getSellingPrice() : null);
        product.setCategory(request.getCategory());
        product.setCondition(request.getCondition());
        product.setListingType(request.getListingType());
        product.setPickupLocation(request.getPickupLocation());

        product.getImages().clear();
        if (request.getImages() != null) {
            List<ProductImage> images = new ArrayList<>();
            int order = 0;
            for (String url : request.getImages()) {
                if (url != null && !url.isBlank()) {
                    images.add(new ProductImage(product, url, order++));
                }
            }
            product.getImages().addAll(images);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
