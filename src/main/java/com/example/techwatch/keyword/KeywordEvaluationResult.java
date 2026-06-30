package com.example.techwatch.keyword;

public record KeywordEvaluationResult(double trendScore, double stabilityScore,
                                      double learningValueScore, double buzzRiskScore,
                                      double finalScore, String status, String recommendation) {
}
