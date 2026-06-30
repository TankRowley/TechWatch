package com.example.techwatch.app;

import java.util.Set;

public record UserSetupSelection(String displayName, String primaryGoal, Set<String> learningKeywords,
                                 Set<String> pinnedKeywords, Set<String> interestCategories) {
    public UserSetupSelection {
        displayName = displayName == null ? "" : displayName.trim();
        primaryGoal = primaryGoal == null ? "" : primaryGoal.trim();
        learningKeywords = learningKeywords == null ? Set.of() : Set.copyOf(learningKeywords);
        pinnedKeywords = pinnedKeywords == null ? Set.of() : Set.copyOf(pinnedKeywords);
        interestCategories = interestCategories == null ? Set.of() : Set.copyOf(interestCategories);
    }
}
