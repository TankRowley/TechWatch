package com.example.techwatch.app;

import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordTrendEvaluator;

import java.time.LocalDate;
import java.util.List;

public class KeywordHistoryService {
    private final KeywordRepository keywords;
    private final KeywordWeeklyStatsRepository stats;
    private final KeywordTrendEvaluator evaluator;

    public KeywordHistoryService(KeywordRepository keywords, KeywordWeeklyStatsRepository stats,
                                 KeywordTrendEvaluator evaluator) {
        this.keywords = keywords;
        this.stats = stats;
        this.evaluator = evaluator;
    }

    public List<Keyword> captureAndEvaluate(LocalDate weekStart) throws Exception {
        stats.capture(weekStart);
        for (Keyword keyword : keywords.findAll()) {
            String trendState = evaluator.evaluate(stats.findRecent(keyword.getId(), 26));
            String status = nextStatus(keyword, trendState);
            keywords.updateHistoryEvaluation(keyword.getId(), trendState, status);
        }
        return keywords.findAll();
    }

    private String nextStatus(Keyword keyword, String trendState) {
        if (!"Dormant".equals(trendState) || keyword.isPinned() || keyword.isLearning()
                || "Core".equals(keyword.getStatus())) return keyword.getStatus();
        if ("Watch".equals(keyword.getStatus())) return "Decline";
        if ("Decline".equals(keyword.getStatus())) return "Ignore";
        return keyword.getStatus();
    }
}
