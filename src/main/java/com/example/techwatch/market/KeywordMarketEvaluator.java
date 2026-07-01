package com.example.techwatch.market;

import com.example.techwatch.keyword.Keyword;

import java.time.LocalDate;
import java.util.List;

public class KeywordMarketEvaluator {
    public KeywordMarketStats evaluate(Keyword keyword, LocalDate weekStart, int usCount, int jpCount,
                                       List<KeywordMarketStats> history) {
        double us4 = growth(usCount, tailAverage(history, 4, true));
        double jp4 = growth(jpCount, tailAverage(history, 4, false));
        double us12 = growth(usCount, tailAverage(history, 12, true));
        double jp12 = growth(jpCount, tailAverage(history, 12, false));
        double usScore = countScore(usCount) + growthScore(us4);
        double jpScore = countScore(jpCount) + growthScore(jp4);
        double global = Math.min(100, usScore * 0.55 + jpScore * 0.45 + (usCount > 0 && jpCount > 0 ? 8 : 0));
        String label = label(keyword, usCount, jpCount, us4, jp4);
        return new KeywordMarketStats(keyword.getId(), weekStart, usCount, jpCount, us4, jp4, us12, jp12,
                usScore, jpScore, global, label, "OBSERVED");
    }

    private String label(Keyword keyword, int us, int jp, double usGrowth, double jpGrowth) {
        if (us == 0 && jp == 0) return "Unknown";
        if (usGrowth < -0.2 && jpGrowth < -0.2) return "Declining Demand";
        if (keyword.getTrendScore() >= 6 && us < 500 && jp < 200) return "Buzz Only";
        if (us >= 1000 && jp >= 500) return "Rising".equals(keyword.getTrendState()) ? "Hot" : "Stable Demand";
        if (us >= 1000 && jp < 500) return "US Leading";
        if (jp >= 500) return "JP Strong";
        if (usGrowth > 0.2 || jpGrowth > 0.2 || "Rising".equals(keyword.getTrendState())) return "Emerging";
        return "Stable Demand";
    }

    private double countScore(int count) { return Math.min(75, Math.log10(count + 1) * 22); }
    private double growthScore(double growth) { return Math.max(-15, Math.min(25, growth * 25)); }
    private double growth(int current, double previous) {
        if (previous <= 0) return 0;
        return (current - previous) / previous;
    }
    private double tailAverage(List<KeywordMarketStats> history, int count, boolean us) {
        if (history == null || history.isEmpty()) return 0;
        return history.subList(Math.max(0, history.size() - count), history.size()).stream()
                .mapToInt(value -> us ? value.usJobCount() : value.jpJobCount()).average().orElse(0);
    }
}
