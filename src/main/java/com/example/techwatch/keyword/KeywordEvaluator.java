package com.example.techwatch.keyword;

import com.example.techwatch.db.KeywordStats;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

public class KeywordEvaluator {
    private static final Set<String> CORE_PROTECTED = Set.of(
            "java", "jvm", "http", "sql", "linux", "git", "docker", "network", "database", "security");

    public KeywordEvaluationResult evaluate(Keyword keyword, KeywordStats stats, Instant now) {
        double trend = stats.currentCount();
        double stability = stats.activeWeeks();
        double learning = learningValue(keyword.getStatus());
        boolean spike = stats.currentCount() >= 3 && stats.currentCount() >= Math.max(3, stats.previousCount() * 2);
        double buzzRisk = spike && stats.sourceDiversity() <= 1 ? Math.min(5, 2 + stats.currentCount() / 2.0) : 0;
        String status = normalizedStatus(keyword.getStatus());
        boolean protectedCore = CORE_PROTECTED.contains(keyword.getNormalizedName().toLowerCase(Locale.ROOT));

        if (protectedCore) {
            status = "Core";
            buzzRisk = Math.min(buzzRisk, 1);
        } else if (spike && stats.sourceDiversity() <= 1) {
            status = "Buzz";
        } else if (("Candidate".equalsIgnoreCase(status) || "Ignore".equalsIgnoreCase(status))
                && stats.currentCount() >= 3 && stats.sourceDiversity() >= 2 && stats.averageArticleScore() >= 6) {
            status = "Watch";
        } else if (keyword.getLastSeenAt() != null && stats.currentCount() == 0
                && Duration.between(keyword.getLastSeenAt(), now).toDays() >= 28) {
            status = "Decline";
        }
        double finalScore = Math.round((trend + stability + learning - buzzRisk) * 10.0) / 10.0;
        return new KeywordEvaluationResult(trend, stability, learning, buzzRisk, finalScore, status,
                recommendation(status, finalScore));
    }

    private double learningValue(String status) {
        if ("Core".equalsIgnoreCase(status)) return 5;
        if ("Watch".equalsIgnoreCase(status)) return 3;
        if ("Buzz".equalsIgnoreCase(status)) return 1;
        return 2;
    }

    private String normalizedStatus(String value) {
        if (value == null || value.isBlank()) return "Candidate";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    public String recommendation(String status, double score) {
        return switch (status) {
            case "Core" -> "基礎として継続学習";
            case "Watch" -> score >= 6 ? "次の学習候補" : "継続監視";
            case "Buzz" -> "追跡するが固執しない";
            case "Decline" -> "優先度を下げる";
            default -> "必要時のみ確認";
        };
    }
}
