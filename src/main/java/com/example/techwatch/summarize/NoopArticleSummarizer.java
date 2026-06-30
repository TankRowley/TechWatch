package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;

import java.util.List;

public class NoopArticleSummarizer implements ArticleSummarizer {
    @Override
    public ArticleSummary summarize(Article article, String bodyText) {
        String summary = article.getSummaryOriginal();
        if ((summary == null || summary.isBlank()) && bodyText != null) {
            summary = bodyText.substring(0, Math.min(bodyText.length(), 500));
        }
        return new ArticleSummary(summary, List.of(), "", "Watch Only", List.of(), List.of(),
                article.getImportanceLabel());
    }
}
