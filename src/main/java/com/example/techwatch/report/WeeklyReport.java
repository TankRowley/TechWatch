package com.example.techwatch.report;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReport(LocalDate periodStart, LocalDate periodEnd, List<Article> articles,
                           List<Keyword> keywords, Map<Long, ArticleSummary> summaries) {
    public WeeklyReport {
        articles = articles == null ? List.of() : List.copyOf(articles);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        summaries = summaries == null ? Map.of() : Map.copyOf(summaries);
    }
}
