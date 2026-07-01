package com.example.techwatch.app;

import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.market.KeywordMarketStats;

import java.util.List;
import java.util.Map;

public class KeywordPriorityService {
    private final KeywordRepository keywords;

    public KeywordPriorityService(KeywordRepository keywords) { this.keywords = keywords; }

    public List<Keyword> evaluate(List<Keyword> values, Map<Long, KeywordMarketStats> marketStats) throws Exception {
        for (Keyword keyword : values) {
            KeywordMarketStats market = marketStats == null ? null : marketStats.get(keyword.getId());
            boolean hasMarket = market != null && market.isObserved()
                    && (market.usJobCount() > 0 || market.jpJobCount() > 0);
            double evidence = hasMarket
                    ? keyword.getActivityScore() * 0.50 + keyword.getStabilityScore() * 0.20
                        + market.globalMarketScore() * 0.30
                    : keyword.getActivityScore() * 0.65 + keyword.getStabilityScore() * 0.35;
            evidence -= keyword.getBuzzRiskScore() * 0.25;
            evidence *= 0.5 + keyword.getConfidenceScore() / 200.0;
            if (keyword.isFoundation()) evidence = Math.max(45, evidence);
            double personalBonus = (keyword.isLearning() ? 15 : 0) + (keyword.isPinned() ? 10 : 0);
            double priority = round(clamp(evidence + personalBonus, 0, 100));
            keywords.updatePriority(keyword.getId(), personalBonus, priority);
        }
        return keywords.findAll();
    }

    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private double round(double value) { return Math.round(value * 10.0) / 10.0; }
}
