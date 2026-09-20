package com.camploop.model.enums;

public enum Condition {
    NEW("New"),
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair"),
    WELL_USED("Well Used");

    private final String displayName;

    Condition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
