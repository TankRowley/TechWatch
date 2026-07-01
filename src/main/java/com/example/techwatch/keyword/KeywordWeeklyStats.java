package com.example.techwatch.keyword;

import java.time.LocalDate;

public record KeywordWeeklyStats(long keywordId, LocalDate weekStart, int mentionCount, int sourceCount,
                                 int officialSourceCount, int highScoreArticleCount,
                                 int reportIncludedCount, double averageArticleScore,
                                 int totalArticleCount, int successfulSourceCount,
                                 int configuredSourceCount, String collectionStatus,
                                 double sourceConcentration, double categoryConcentration) {
    public KeywordWeeklyStats {
        collectionStatus = collectionStatus == null || collectionStatus.isBlank() ? "LEGACY" : collectionStatus;
        sourceConcentration = Math.max(0, Math.min(1, sourceConcentration));
        categoryConcentration = Math.max(0, Math.min(1, categoryConcentration));
    }

    public KeywordWeeklyStats(long keywordId, LocalDate weekStart, int mentionCount, int sourceCount,
                              int officialSourceCount, int highScoreArticleCount,
                              int reportIncludedCount, double averageArticleScore,
                              int totalArticleCount, int successfulSourceCount,
                              int configuredSourceCount, String collectionStatus,
                              double sourceConcentration) {
        this(keywordId, weekStart, mentionCount, sourceCount, officialSourceCount, highScoreArticleCount,
                reportIncludedCount, averageArticleScore, totalArticleCount, successfulSourceCount,
                configuredSourceCount, collectionStatus, sourceConcentration, sourceConcentration);
    }

    public KeywordWeeklyStats(long keywordId, LocalDate weekStart, int mentionCount, int sourceCount,
                              int officialSourceCount, int highScoreArticleCount,
                              int reportIncludedCount, double averageArticleScore) {
        this(keywordId, weekStart, mentionCount, sourceCount, officialSourceCount, highScoreArticleCount,
                reportIncludedCount, averageArticleScore, 0, 0, 0, "LEGACY",
                mentionCount > 0 && sourceCount > 0 ? 1.0 / sourceCount : 1.0,
                mentionCount > 0 && sourceCount > 0 ? 1.0 / sourceCount : 1.0);
    }

    public boolean isObserved() {
        return totalArticleCount > 0 && !"MISSING".equalsIgnoreCase(collectionStatus);
    }

    public double mentionRate() {
        return totalArticleCount <= 0 ? 0 : mentionCount * 100.0 / totalArticleCount;
    }

    public double evidenceConcentration() { return Math.max(sourceConcentration, categoryConcentration); }

    public double collectionCoverage() {
        if ("MISSING".equalsIgnoreCase(collectionStatus)) return 0;
        if ("HISTORICAL_PARTIAL".equalsIgnoreCase(collectionStatus)) {
            if (configuredSourceCount <= 0) return 0.5;
            return Math.min(0.5, successfulSourceCount / (double) configuredSourceCount);
        }
        if (configuredSourceCount <= 0) return "LEGACY".equalsIgnoreCase(collectionStatus) ? 0.75 : 1;
        return Math.max(0, Math.min(1, successfulSourceCount / (double) configuredSourceCount));
    }
}
