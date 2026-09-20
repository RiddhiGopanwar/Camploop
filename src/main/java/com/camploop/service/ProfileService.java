package com.camploop.service;

import java.util.UUID;

import com.camploop.config.CurrentUser;
import com.camploop.exception.ApiException;
import com.camploop.model.Profile;
import com.camploop.repository.ProfileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Autowired
    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Supabase Auth creates the user; Camploop's own profiles row is created
     * lazily the first time that user hits the backend.
     */
    public Profile getOrCreate(CurrentUser currentUser, String fallbackName) {

        UUID userId = UUID.fromString(currentUser.getId());

        return profileRepository.findById(userId)
                .orElseGet(() -> {
                    Profile profile = new Profile();

                    profile.setId(userId);
                    profile.setEmail(currentUser.getEmail());

                    profile.setName(
                            fallbackName != null && !fallbackName.isBlank()
                                    ? fallbackName
                                    : (currentUser.getEmail() != null
                                            ? currentUser.getEmail().split("@")[0]
                                            : "Student")
                    );

                    return profileRepository.save(profile);
                });
    }

    public Profile getById(String id) {

        return profileRepository.findById(UUID.fromString(id))
                .orElseThrow(() ->
                        new ApiException("Profile not found", HttpStatus.NOT_FOUND)
                );
    }

    public Profile update(
            String id,
            String name,
            String college,
            String profileImage) {

        Profile profile = getById(id);

        if (name != null && !name.isBlank()) {
            profile.setName(name);
        }

        if (college != null) {
            profile.setCollege(college);
        }

        if (profileImage != null) {
            profile.setProfileImage(profileImage);
        }

        return profileRepository.save(profile);
    }
}