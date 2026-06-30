package com.example.techwatch.market;

import java.time.LocalDate;

public record KeywordMarketStats(long keywordId, LocalDate weekStart, int usJobCount, int jpJobCount,
                                 double usGrowth4w, double jpGrowth4w, double usGrowth12w,
                                 double jpGrowth12w, double usMarketScore, double jpMarketScore,
                                 double globalMarketScore, String marketLabel) { }
