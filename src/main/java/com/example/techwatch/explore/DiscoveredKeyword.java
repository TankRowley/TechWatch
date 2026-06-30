package com.example.techwatch.explore;

import java.time.Instant;
import java.util.List;

public record DiscoveredKeyword(Long id, String name, String normalizedName, String category,
                                String description, String learningJudgement, List<String> prerequisites,
                                Instant firstSeenAt, Instant lastSeenAt, int mentionCount,
                                boolean promotedToKeyword) {
    public DiscoveredKeyword {
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
    }
}
