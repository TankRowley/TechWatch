package com.example.techwatch.article;

import java.util.List;

public record ArticleScore(double score, String label, List<String> matchedKeywords, String reason) {
    public ArticleScore {
        matchedKeywords = matchedKeywords == null ? List.of() : List.copyOf(matchedKeywords);
        label = label == null ? "Ignore" : label;
        reason = reason == null ? "" : reason;
    }
}
