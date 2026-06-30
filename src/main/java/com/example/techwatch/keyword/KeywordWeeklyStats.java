package com.example.techwatch.keyword;

import java.time.LocalDate;

public record KeywordWeeklyStats(long keywordId, LocalDate weekStart, int mentionCount, int sourceCount,
                                 int officialSourceCount, int highScoreArticleCount,
                                 int reportIncludedCount, double averageArticleScore) { }
