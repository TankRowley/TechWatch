package com.example.techwatch.keyword;

import java.util.List;

public class KeywordTrendEvaluator {
    public String evaluate(List<KeywordWeeklyStats> history) {
        if (history == null || history.isEmpty()) return "Dormant";
        List<KeywordWeeklyStats> lastEight = tail(history, 8);
        int eightTotal = total(lastEight);
        if (eightTotal <= 1 && lastEight.stream().mapToInt(KeywordWeeklyStats::reportIncludedCount).sum() == 0
                && lastEight.stream().mapToInt(KeywordWeeklyStats::highScoreArticleCount).sum() == 0) return "Dormant";

        List<KeywordWeeklyStats> lastFour = tail(history, 4);
        KeywordWeeklyStats current = lastFour.get(lastFour.size() - 1);
        double previousAverage = lastFour.size() <= 1 ? 0 : lastFour.subList(0, lastFour.size() - 1)
                .stream().mapToInt(KeywordWeeklyStats::mentionCount).average().orElse(0);
        if (current.mentionCount() > Math.max(1, previousAverage * 1.35)
                && (current.sourceCount() >= 2 || current.highScoreArticleCount() > 0)) return "Rising";

        List<KeywordWeeklyStats> older = history.size() <= 4 ? List.of() : history.subList(0, history.size() - 4);
        double olderAverage = older.stream().mapToInt(KeywordWeeklyStats::mentionCount).average().orElse(0);
        double recentAverage = lastFour.stream().mapToInt(KeywordWeeklyStats::mentionCount).average().orElse(0);
        if (olderAverage >= 1 && recentAverage < olderAverage * 0.55) return "Cooling";

        long activeWeeks = lastFour.stream().filter(value -> value.mentionCount() > 0).count();
        if (activeWeeks >= 3 || (history.size() >= 8 && eightTotal >= 8)) return "Stable";
        return current.mentionCount() > 0 ? "Rising" : "Cooling";
    }

    private List<KeywordWeeklyStats> tail(List<KeywordWeeklyStats> values, int count) {
        return values.subList(Math.max(0, values.size() - count), values.size());
    }

    private int total(List<KeywordWeeklyStats> values) {
        return values.stream().mapToInt(KeywordWeeklyStats::mentionCount).sum();
    }
}
