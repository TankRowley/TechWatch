package com.example.techwatch.keyword;

import java.util.Set;

public record KeywordMatch(Keyword keyword, Set<String> detectedIn) {
    public KeywordMatch {
        detectedIn = detectedIn == null ? Set.of() : Set.copyOf(detectedIn);
    }
}
