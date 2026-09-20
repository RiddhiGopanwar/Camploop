package com.camploop.dto;

import com.camploop.model.Profile;

import java.util.UUID;

/** The logged-in student's own view of their profile — includes email, unlike ProfileResponse. */
public class MyProfileResponse {
    private UUID id;
    private String name;
    private String email;
    private String college;
    private String profileImage;

    public MyProfileResponse(Profile profile) {
        this.id = profile.getId();
        this.name = profile.getName();
        this.email = profile.getEmail();
        this.college = profile.getCollege();
        this.profileImage = profile.getProfileImage();
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getCollege() { return college; }

    public String getProfileImage() { return profileImage; }
}