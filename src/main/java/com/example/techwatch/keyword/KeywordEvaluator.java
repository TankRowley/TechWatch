package com.example.techwatch.keyword;

import com.example.techwatch.db.KeywordStats;

import java.time.Instant;
import java.util.Locale;

public class KeywordEvaluator {
    public KeywordEvaluationResult evaluate(Keyword keyword, KeywordStats stats, Instant now) {
        double trend = stats.currentCount();
        double stability = stats.activeWeeks();
        double learning = (keyword.isLearning() ? 15 : 0) + (keyword.isPinned() ? 10 : 0);
        boolean spike = stats.currentCount() >= 3 && stats.currentCount() >= Math.max(3, stats.previousCount() * 2);
        double buzzRisk = spike && stats.sourceDiversity() <= 1 ? Math.min(5, 2 + stats.currentCount() / 2.0) : 0;
        String status = normalizedStatus(keyword.getStatus());
        boolean protectedCore = keyword.isFoundation();

        if (protectedCore) {
            status = "Core";
            buzzRisk = Math.min(buzzRisk, 1);
        }
        double finalScore = Math.round((trend + stability + learning - buzzRisk) * 10.0) / 10.0;
        return new KeywordEvaluationResult(trend, stability, learning, buzzRisk, finalScore, status,
                recommendation(status, finalScore));
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
