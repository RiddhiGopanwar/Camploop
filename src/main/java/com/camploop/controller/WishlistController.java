package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.config.CurrentUser;
import com.camploop.dto.ApiMessageResponse;
import com.camploop.dto.WishlistResponse;
import com.camploop.model.Profile;
import com.camploop.service.ProfileService;
import com.camploop.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final ProfileService profileService;
    private final AuthContext authContext;

    @Autowired
    public WishlistController(WishlistService wishlistService, ProfileService profileService, AuthContext authContext) {
        this.wishlistService = wishlistService;
        this.profileService = profileService;
        this.authContext = authContext;
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> myWishlist(HttpServletRequest httpRequest) {
        Profile user = currentProfile(httpRequest);
        List<WishlistResponse> items = wishlistService.getByUser(user).stream()
                .map(WishlistResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponse> add(@PathVariable Long productId, HttpServletRequest httpRequest) {
        Profile user = currentProfile(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new WishlistResponse(wishlistService.add(productId, user)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiMessageResponse> remove(@PathVariable Long productId, HttpServletRequest httpRequest) {
        Profile user = currentProfile(httpRequest);
        wishlistService.remove(productId, user);
        return ResponseEntity.ok(new ApiMessageResponse("Removed from wishlist"));
    }

    private Profile currentProfile(HttpServletRequest httpRequest) {
        CurrentUser currentUser = authContext.require(httpRequest);
        return profileService.getOrCreate(currentUser, null);
    }
}
