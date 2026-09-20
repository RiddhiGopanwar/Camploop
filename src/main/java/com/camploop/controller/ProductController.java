package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.config.CurrentUser;
import com.camploop.dto.ApiMessageResponse;
import com.camploop.dto.ProductRequest;
import com.camploop.dto.ProductResponse;
import com.camploop.model.Product;
import com.camploop.model.Profile;
import com.camploop.model.enums.Category;
import com.camploop.model.enums.Condition;
import com.camploop.model.enums.ListingStatus;
import com.camploop.service.ProductService;
import com.camploop.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProfileService profileService;
    private final AuthContext authContext;

    @Autowired
    public ProductController(ProductService productService, ProfileService profileService, AuthContext authContext) {
        this.productService = productService;
        this.profileService = profileService;
        this.authContext = authContext;
    }

    /** Marketplace preview on the homepage. */
    @GetMapping("/recent")
    public ResponseEntity<List<ProductResponse>> recent() {
        return ResponseEntity.ok(toResponses(productService.getRecent()));
    }

    /** Browse / search / filter / sort. Only ever returns AVAILABLE listings — a SOLD
     *  or UNAVAILABLE item stays in its seller's history but drops out of results here. */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Condition condition,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "newest") String sort
    ) {
        List<Product> results = productService.search(keyword, category, condition, minPrice, maxPrice);

        if ("price_low".equals(sort)) {
            results.sort((a, b) -> compareNullable(a.getSellingPrice(), b.getSellingPrice()));
        } else if ("price_high".equals(sort)) {
            results.sort((a, b) -> compareNullable(b.getSellingPrice(), a.getSellingPrice()));
        } else {
            results.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        return ResponseEntity.ok(toResponses(results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(new ProductResponse(productService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request, HttpServletRequest httpRequest) {
        Profile seller = currentProfile(httpRequest);
        Product product = productService.create(request, seller);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProductResponse(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request,
                                                    HttpServletRequest httpRequest) {
        Profile currentUser = currentProfile(httpRequest);
        Product product = productService.update(id, request, currentUser);
        return ResponseEntity.ok(new ProductResponse(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        Profile currentUser = currentProfile(httpRequest);
        productService.delete(id, currentUser);
        return ResponseEntity.ok(new ApiMessageResponse("Listing deleted"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ProductResponse> markStatus(@PathVariable Long id, @RequestParam ListingStatus status,
                                                        HttpServletRequest httpRequest) {
        Profile currentUser = currentProfile(httpRequest);
        Product product = productService.markStatus(id, status, currentUser);
        return ResponseEntity.ok(new ProductResponse(product));
    }

    /** The current student's own listings (all statuses), for their My Listings dashboard. */
    @GetMapping("/mine")
    public ResponseEntity<List<ProductResponse>> mine(HttpServletRequest httpRequest) {
        Profile currentUser = currentProfile(httpRequest);
        return ResponseEntity.ok(toResponses(productService.getBySeller(currentUser)));
    }

    private Profile currentProfile(HttpServletRequest httpRequest) {
        CurrentUser currentUser = authContext.require(httpRequest);
        return profileService.getOrCreate(currentUser, null);
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        return products.stream().map(ProductResponse::new).collect(Collectors.toList());
    }

    private int compareNullable(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }
}
