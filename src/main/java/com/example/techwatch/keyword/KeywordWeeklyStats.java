package com.example.techwatch.keyword;

import java.time.LocalDate;

public record KeywordWeeklyStats(long keywordId, LocalDate weekStart, int mentionCount, int sourceCount,
                                 int officialSourceCount, int highScoreArticleCount,
                                 int reportIncludedCount, double averageArticleScore,
                                 int totalArticleCount, int successfulSourceCount,
                                 int configuredSourceCount, String collectionStatus,
                                 double sourceConcentration) {
    public KeywordWeeklyStats {
        collectionStatus = collectionStatus == null || collectionStatus.isBlank() ? "LEGACY" : collectionStatus;
        sourceConcentration = Math.max(0, Math.min(1, sourceConcentration));
    }

    public KeywordWeeklyStats(long keywordId, LocalDate weekStart, int mentionCount, int sourceCount,
                              int officialSourceCount, int highScoreArticleCount,
                              int reportIncludedCount, double averageArticleScore) {
        this(keywordId, weekStart, mentionCount, sourceCount, officialSourceCount, highScoreArticleCount,
                reportIncludedCount, averageArticleScore, 0, 0, 0, "LEGACY",
                mentionCount > 0 && sourceCount > 0 ? 1.0 / sourceCount : 1.0);
    }

    public boolean isObserved() {
        return totalArticleCount > 0 && !"MISSING".equalsIgnoreCase(collectionStatus);
    }

    public double mentionRate() {
        return totalArticleCount <= 0 ? 0 : mentionCount * 100.0 / totalArticleCount;
    }

    public double collectionCoverage() {
        if ("MISSING".equalsIgnoreCase(collectionStatus)) return 0;
        if (configuredSourceCount <= 0) return "LEGACY".equalsIgnoreCase(collectionStatus) ? 0.75 : 1;
        return Math.max(0, Math.min(1, successfulSourceCount / (double) configuredSourceCount));
    }
}
