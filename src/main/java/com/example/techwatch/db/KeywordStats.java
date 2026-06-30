package com.example.techwatch.db;

public record KeywordStats(long keywordId, int currentCount, int previousCount,
                           int activeWeeks, int sourceDiversity, double averageArticleScore) {
}
