package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;

import java.nio.file.Path;
import java.util.List;

public record WeeklyRunResult(Path reportPath, String reportMarkdown, List<Article> articles,
                              List<Keyword> keywords, List<String> logs, ArticleRunStats stats) {
    public WeeklyRunResult {
        reportMarkdown = reportMarkdown == null ? "" : reportMarkdown;
        articles = articles == null ? List.of() : List.copyOf(articles);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        logs = logs == null ? List.of() : List.copyOf(logs);
        stats = stats == null ? new ArticleRunStats(0, 0, 0, 0, 0, 0, 0) : stats;
    }
}
