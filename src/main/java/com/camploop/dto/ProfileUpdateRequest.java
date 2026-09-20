package com.camploop.dto;

public class ProfileUpdateRequest {
    private String name;
    private String college;
    private String profileImage;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}
