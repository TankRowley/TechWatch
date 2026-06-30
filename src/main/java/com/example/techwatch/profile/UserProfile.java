package com.example.techwatch.profile;

public record UserProfile(Long id, String displayName, String primaryGoal, String experienceLevel) {
    public UserProfile {
        displayName = displayName == null ? "" : displayName.trim();
        primaryGoal = primaryGoal == null ? "" : primaryGoal.trim();
        experienceLevel = experienceLevel == null || experienceLevel.isBlank() ? "beginner" : experienceLevel;
    }
}
