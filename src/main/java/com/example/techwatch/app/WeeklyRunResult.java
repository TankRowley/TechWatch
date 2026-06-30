package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record WeeklyRunResult(Path reportPath, String reportMarkdown, List<Article> articles,
                              List<Keyword> keywords, Map<Long, ArticleSummary> summaries,
                              List<String> logs, ArticleRunStats stats) {
    public WeeklyRunResult {
        reportMarkdown = reportMarkdown == null ? "" : reportMarkdown;
        articles = articles == null ? List.of() : List.copyOf(articles);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        summaries = summaries == null ? Map.of() : Map.copyOf(summaries);
        logs = logs == null ? List.of() : List.copyOf(logs);
        stats = stats == null ? new ArticleRunStats(0, 0, 0, 0, 0, 0, 0) : stats;
    }

    public WeeklyRunResult(Path reportPath, String reportMarkdown, List<Article> articles,
                           List<Keyword> keywords, List<String> logs, ArticleRunStats stats) {
        this(reportPath, reportMarkdown, articles, keywords, Map.of(), logs, stats);
    }
}
