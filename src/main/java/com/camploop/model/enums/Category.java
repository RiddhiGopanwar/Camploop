package com.camploop.model.enums;

public enum Category {
    BOOKS_TEXTBOOKS("Books & Textbooks"),
    ELECTRONICS("Electronics"),
    CALCULATORS("Calculators"),
    HOSTEL_ESSENTIALS("Hostel Essentials"),
    FASHION("Fashion"),
    BAGS_ACCESSORIES("Bags & Accessories"),
    CYCLES("Cycles"),
    GAMING("Gaming"),
    ACADEMIC_SUPPLIES("Academic Supplies"),
    OTHERS("Others");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
