package com.camploop.dto;

import com.camploop.model.Profile;

import java.util.UUID;

/** Public-safe profile view — no email/private data beyond what's needed to trust a seller. */
public class ProfileResponse {
    private UUID id;
    private String name;
    private String college;
    private String profileImage;
    private int activeListings;
    private int soldListings;

    public ProfileResponse(Profile profile, int activeListings, int soldListings) {
        this.id = profile.getId();
        this.name = profile.getName();
        this.college = profile.getCollege();
        this.profileImage = profile.getProfileImage();
        this.activeListings = activeListings;
        this.soldListings = soldListings;
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getCollege() { return college; }

    public String getProfileImage() { return profileImage; }

    public int getActiveListings() { return activeListings; }

    public int getSoldListings() { return soldListings; }
}