package com.example.techwatch.score;

import com.example.techwatch.article.Article;
import com.example.techwatch.article.ArticleScore;
import com.example.techwatch.keyword.KeywordMatch;
import com.example.techwatch.source.Source;

import java.util.ArrayList;
import java.util.List;

public class KeywordBasedArticleScorer implements ArticleScorer {
    @Override
    public ArticleScore score(Article article, Source source, List<KeywordMatch> matches) {
        double score = source.trustScore();
        boolean hasCore = false;
        boolean buzzOnly = !matches.isEmpty();
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
            if ("Core".equalsIgnoreCase(keyword.getStatus())) hasCore = true;
            if (!"Buzz".equalsIgnoreCase(keyword.getStatus())) buzzOnly = false;
        }
        if (hasCore) { score += 1; reasons.add("Core技術 +1"); }
        if (buzzOnly) { score -= 2; reasons.add("Buzzのみ -2"); }
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
