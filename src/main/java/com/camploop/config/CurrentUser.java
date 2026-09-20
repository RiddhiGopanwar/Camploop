package com.camploop.config;

/** The authenticated Supabase user for the current request, extracted from their JWT. */
public class CurrentUser {
    private final String id;    // Supabase auth.users.id (UUID) — also profiles.id
    private final String email;

    public CurrentUser(String id, String email) {
        this.id = id;
        this.email = email;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
}
