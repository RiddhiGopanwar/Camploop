package com.camploop.service;

import com.camploop.dto.ProductRequest;
import com.camploop.exception.ApiException;
import com.camploop.model.Product;
import com.camploop.model.ProductImage;
import com.camploop.model.Profile;
import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.PurchaseRequest;
import com.camploop.model.enums.ListingStatus;
import com.camploop.model.enums.RequestStatus;
import com.camploop.repository.ProductRepository;
import com.camploop.repository.PurchaseRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;

    @Autowired
    public ProductService(ProductRepository productRepository,
                          PurchaseRequestRepository purchaseRequestRepository) {
        this.productRepository = productRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
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
        assertPriceNotChanged(product, request);
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
    @Transactional
    public Product markStatus(Long productId, ListingStatus status, Profile currentUser) {
        Product product = getById(productId);
        assertOwner(product, currentUser);
        product.setStatus(status);
        product = productRepository.save(product);

        // A SOLD / UNAVAILABLE item can't be bought any more, so close any open buy requests.
        if (status != ListingStatus.AVAILABLE) {
            List<PurchaseRequest> pending =
                    purchaseRequestRepository.findByProductAndStatus(product, RequestStatus.PENDING);
            pending.forEach(r -> r.setStatus(RequestStatus.CLOSED));
            purchaseRequestRepository.saveAll(pending);
        }
        return product;
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

    /**
     * FIXED PRICE RULE: once a listing has been published with a selling price, that price
     * (and the SELL listing type that goes with it) can never change. Rejecting here gives the
     * user a clear message; the same rule is also enforced by a database trigger
     * (sql/002_buy_chat_pricelock.sql) so it can't be bypassed by calling Supabase directly.
     */
    private void assertPriceNotChanged(Product product, ProductRequest request) {
        if (product.getSellingPrice() == null) {
            return; // never published with a price (Exchange / Donate) — nothing to lock
        }
        BigDecimal requested = request.getSellingPrice();
        boolean priceChanged = requested != null && requested.compareTo(product.getSellingPrice()) != 0;
        boolean typeChanged = request.getListingType() != null && request.getListingType() != product.getListingType();
        if (priceChanged || typeChanged) {
            throw new ApiException(
                    "The selling price is fixed once a listing is published and can't be changed",
                    HttpStatus.CONFLICT);
        }
    }

    private void applyRequest(Product product, ProductRequest request) {
        // Capture the locked values BEFORE overwriting anything (null for brand-new listings)
        BigDecimal lockedPrice = product.getSellingPrice();
        var lockedType = product.getListingType();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setCategory(request.getCategory());
        product.setCondition(request.getCondition());
        if (lockedPrice != null) {
            // Existing priced listing: keep price + type exactly as published
            product.setSellingPrice(lockedPrice);
            product.setListingType(lockedType);
        } else {
            // New listing (or one that never had a price). Exchange/Donate carry no selling price.
            product.setListingType(request.getListingType());
            product.setSellingPrice(request.getListingType() != null && request.getListingType().name().equals("SELL")
                    ? request.getSellingPrice() : null);
        }
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
