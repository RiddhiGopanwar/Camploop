package com.camploop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * A registered student's profile. There is only ONE account type in Camploop —
 * every profile can both buy and sell. No separate buyer/seller roles.
 *
 * The id is NOT auto-generated here: it is set to match the Supabase Auth
 * user's id (auth.users.id) the first time we see a request from them, so
 * profiles.id doubles as the foreign key every other table references.
 * Supabase Auth itself owns the password — this table never stores one.
 */
@Entity
@Table(name = "profiles")
public class Profile {

@Id
private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 100)
    private String college;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Wishlist> wishlistItems = new ArrayList<>();

    public Profile() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Product> getProducts() { return products; }
    public List<Wishlist> getWishlistItems() { return wishlistItems; }
}
