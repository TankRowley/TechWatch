package com.example.techwatch.market;

import java.time.LocalDate;

public record KeywordMarketStats(long keywordId, LocalDate weekStart, int usJobCount, int jpJobCount,
                                 double usGrowth4w, double jpGrowth4w, double usGrowth12w,
                                 double jpGrowth12w, double usMarketScore, double jpMarketScore,
                                 double globalMarketScore, String marketLabel, String observationStatus) {
    public KeywordMarketStats {
        observationStatus = observationStatus == null || observationStatus.isBlank()
                ? "LEGACY" : observationStatus;
    }

    public KeywordMarketStats(long keywordId, LocalDate weekStart, int usJobCount, int jpJobCount,
                              double usGrowth4w, double jpGrowth4w, double usGrowth12w,
                              double jpGrowth12w, double usMarketScore, double jpMarketScore,
                              double globalMarketScore, String marketLabel) {
        this(keywordId, weekStart, usJobCount, jpJobCount, usGrowth4w, jpGrowth4w,
                usGrowth12w, jpGrowth12w, usMarketScore, jpMarketScore,
                globalMarketScore, marketLabel, "LEGACY");
    }

    public boolean isObserved() {
        return "OBSERVED".equalsIgnoreCase(observationStatus)
                || "LEGACY".equalsIgnoreCase(observationStatus);
    }

    public KeywordMarketStats asStale() {
        return new KeywordMarketStats(keywordId, weekStart, usJobCount, jpJobCount, usGrowth4w,
                jpGrowth4w, usGrowth12w, jpGrowth12w, usMarketScore, jpMarketScore,
                globalMarketScore, marketLabel, "STALE");
    }
}
