package com.example.techwatch.app;

import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordTrendEvaluator;
import com.example.techwatch.keyword.KeywordTrendAssessment;

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
        return captureAndEvaluate(weekStart, 0, 0);
    }

    public List<Keyword> captureAndEvaluate(LocalDate weekStart, int successfulSources,
                                            int configuredSources) throws Exception {
        stats.capture(weekStart, successfulSources, configuredSources);
        for (Keyword keyword : keywords.findAll()) {
            KeywordTrendAssessment assessment = evaluator.assess(
                    stats.findSince(keyword.getId(), weekStart.minusWeeks(103)));
            LocalDate changedWeek = keywords.findStatusChangedWeek(keyword.getId());
            String status = nextStatus(keyword, assessment, weekStart, changedWeek);
            keywords.updateTrendAssessment(keyword.getId(), assessment, status, weekStart);
        }
        return keywords.findAll();
    }

    private String nextStatus(Keyword keyword, KeywordTrendAssessment assessment,
                              LocalDate weekStart, LocalDate changedWeek) {
        if (keyword.isFoundation()) return "Core";
        if (assessment.confidenceScore() >= 35 && assessment.buzzRiskScore() >= 65
                && !keyword.isLearning() && !keyword.isPinned()) return "Buzz";
        if (("Candidate".equals(keyword.getStatus()) || "Decline".equals(keyword.getStatus())
                || "Ignore".equals(keyword.getStatus()))
                && assessment.confidenceScore() >= 35 && assessment.activityScore() >= 45
                && assessment.buzzRiskScore() < 50
                && ("Rising".equals(assessment.state()) || "Stable".equals(assessment.state()))) return "Watch";
        if (!"Dormant".equals(assessment.state()) || keyword.isPinned() || keyword.isLearning()) {
            if ("Buzz".equals(keyword.getStatus()) && assessment.buzzRiskScore() < 35
                    && assessment.activityScore() >= 35) return "Watch";
            return keyword.getStatus();
        }
        if ("Watch".equals(keyword.getStatus())) return "Decline";
        if ("Buzz".equals(keyword.getStatus())) return "Decline";
        if ("Decline".equals(keyword.getStatus()) && changedWeek != null
                && !changedWeek.isAfter(weekStart.minusWeeks(8))) return "Ignore";
        return keyword.getStatus();
    }
}
