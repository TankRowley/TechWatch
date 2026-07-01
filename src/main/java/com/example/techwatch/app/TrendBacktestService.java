package com.example.techwatch.app;

import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.keyword.KeywordTrendEvaluator;
import com.example.techwatch.keyword.KeywordWeeklyStats;
import java.time.LocalDate;
import java.util.List;

public class TrendBacktestService {
    private final KeywordRepository keywords; private final KeywordWeeklyStatsRepository stats;
    private final KeywordTrendEvaluator evaluator;
    public TrendBacktestService(KeywordRepository keywords, KeywordWeeklyStatsRepository stats, KeywordTrendEvaluator evaluator) {
        this.keywords = keywords; this.stats = stats; this.evaluator = evaluator;
    }
    public TrendBacktestResult evaluate(LocalDate asOf) throws Exception {
        int evaluated=0,rising=0,risingValidated=0,buzz=0,buzzValidated=0;
        for (var keyword : keywords.findAll()) {
            List<KeywordWeeklyStats> history = stats.findSince(keyword.getId(), asOf.minusWeeks(103)).stream()
                    .filter(value -> !value.weekStart().isAfter(asOf)).toList();
            if (history.size() < 13) continue; evaluated++;
            for (int index=3; index+4<history.size(); index++) {
                var assessment=evaluator.assess(history.subList(0,index+1));
                var future=history.subList(index+1,Math.min(history.size(),index+5));
                long active=future.stream().filter(KeywordWeeklyStats::isObserved).filter(v->v.mentionCount()>0).count();
                if (assessment.confidenceScore()>=35 && "Rising".equals(assessment.state())) { rising++; if(active>=2) risingValidated++; }
                if (assessment.confidenceScore()>=35 && assessment.buzzRiskScore()>=65) { buzz++; if(active<2) buzzValidated++; }
            }
        }
        return new TrendBacktestResult(evaluated,rising,risingValidated,buzz,buzzValidated);
    }
}
