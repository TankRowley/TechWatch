package com.example.techwatch.summarize;

import java.util.List;

public record ArticleSummary(String shortSummary, List<String> technicalPoints, String whyItMatters,
                             String learningPriority, List<String> prerequisites,
                             List<String> relatedKeywords, String importanceLabel) {
    public ArticleSummary {
        shortSummary = shortSummary == null ? "" : shortSummary;
        technicalPoints = technicalPoints == null ? List.of() : List.copyOf(technicalPoints);
        whyItMatters = whyItMatters == null ? "" : whyItMatters;
        learningPriority = learningPriority == null ? "Watch Only" : learningPriority;
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        relatedKeywords = relatedKeywords == null ? List.of() : List.copyOf(relatedKeywords);
        importanceLabel = importanceLabel == null ? "UNRATED" : importanceLabel;
    }
}
