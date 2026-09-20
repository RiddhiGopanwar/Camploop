package com.camploop.controller;

import com.camploop.config.AuthContext;
import com.camploop.config.CurrentUser;
import com.camploop.dto.MyProfileResponse;
import com.camploop.dto.ProfileResponse;
import com.camploop.dto.ProfileUpdateRequest;
import com.camploop.model.Profile;
import com.camploop.model.enums.ListingStatus;
import com.camploop.repository.ProductRepository;
import com.camploop.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthContext authContext;
    private final ProductRepository productRepository;

    @Autowired
    public ProfileController(ProfileService profileService, AuthContext authContext, ProductRepository productRepository) {
        this.profileService = profileService;
        this.authContext = authContext;
        this.productRepository = productRepository;
    }

    /** The logged-in student's own profile. Creates the profiles row on first call. */
    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> me(HttpServletRequest httpRequest) {
        CurrentUser currentUser = authContext.require(httpRequest);
        Profile profile = profileService.getOrCreate(currentUser, null);
        return ResponseEntity.ok(new MyProfileResponse(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<MyProfileResponse> updateMe(@RequestBody ProfileUpdateRequest request, HttpServletRequest httpRequest) {
        CurrentUser currentUser = authContext.require(httpRequest);
        Profile profile = profileService.update(currentUser.getId(), request.getName(), request.getCollege(), request.getProfileImage());
        return ResponseEntity.ok(new MyProfileResponse(profile));
    }

    /** Public view of any student's profile — e.g. tapping a seller's name on a listing. */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getOne(@PathVariable String id) {
        Profile profile = profileService.getById(id);
        int active = productRepository.findBySellerAndStatus(profile, ListingStatus.AVAILABLE).size();
        int sold = productRepository.findBySellerAndStatus(profile, ListingStatus.SOLD).size();
        return ResponseEntity.ok(new ProfileResponse(profile, active, sold));
    }
}
