package com.example.techwatch.keyword;

public record KeywordTrendAssessment(String state, double activityScore, double stabilityScore,
                                     double buzzRiskScore, double confidenceScore,
                                     double shortRate, double mediumRate, double longRate) {
}
