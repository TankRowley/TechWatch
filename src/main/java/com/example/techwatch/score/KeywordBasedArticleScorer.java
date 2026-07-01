package com.example.techwatch.score;

import com.example.techwatch.article.Article;
import com.example.techwatch.article.ArticleScore;
import com.example.techwatch.keyword.KeywordMatch;
import com.example.techwatch.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KeywordBasedArticleScorer implements ArticleScorer {
    private final Set<String> interestCategories;

    public KeywordBasedArticleScorer() { this(Set.of()); }

    public KeywordBasedArticleScorer(Set<String> interestCategories) {
        this.interestCategories = interestCategories == null ? Set.of() : interestCategories.stream()
                .map(String::toLowerCase).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public ArticleScore score(Article article, Source source, List<KeywordMatch> matches) {
        double score = source.trustScore();
        boolean hasCore = false;
        boolean buzzOnly = !matches.isEmpty();
        Set<String> rewardedCategories = new java.util.HashSet<>();
        List<String> names = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        reasons.add("情報源信頼度 +" + source.trustScore());

        for (KeywordMatch match : matches) {
            var keyword = match.keyword();
            names.add(keyword.getName());
            if (match.detectedIn().contains("title")) {
                score += keyword.getWeight();
                reasons.add(keyword.getName() + "(title) +" + keyword.getWeight());
            }
            if (match.detectedIn().contains("summary")) {
                double bonus = keyword.getWeight() / 2.0;
                score += bonus;
                reasons.add(keyword.getName() + "(summary) +" + bonus);
            }
            if (match.detectedIn().contains("body")) {
                double bonus = keyword.getWeight() / 4.0; score += bonus;
                reasons.add(keyword.getName() + "(body) +" + bonus);
            }
            if ("Core".equalsIgnoreCase(keyword.getStatus())) hasCore = true;
            if (!"Buzz".equalsIgnoreCase(keyword.getStatus())) buzzOnly = false;
            if (keyword.isLearning()) { score += 2; reasons.add(keyword.getName() + "(学習中) +2"); }
            if (keyword.isPinned()) { score += 2; reasons.add(keyword.getName() + "(固定) +2"); }
            if (interestCategories.contains(keyword.getCategory().toLowerCase())
                    && rewardedCategories.add(keyword.getCategory().toLowerCase())) {
                score += 1;
                reasons.add(keyword.getCategory() + "(興味領域) +1");
            }
        }
        if (hasCore) { score += 1; reasons.add("Core技術 +1"); }
        if (buzzOnly) { score -= 2; reasons.add("Buzzのみ -2"); }
        if (matches.isEmpty()) {
            score = Math.min(score, 4.9);
            reasons.add("関連キーワードなし: 軽く確認以下に制限");
        }
        score = Math.max(0, Math.round(score * 10.0) / 10.0);
        return new ArticleScore(score, label(score), names, String.join(", ", reasons));
    }

    public String label(double score) {
        if (score >= 12) return "Must Read";
        if (score >= 8) return "Watch";
        if (score >= 5) return "Skim";
        if (score >= 2) return "Archive";
        return "Ignore";
    }
}
