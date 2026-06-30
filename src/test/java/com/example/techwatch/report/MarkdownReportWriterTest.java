package com.example.techwatch.report;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownReportWriterTest {
    @Test
    void rendersMustReadAndKeywordTable() {
        Article article = Article.fetched(1L, "Tech Blog", "Java architecture", "https://example.com/a", Instant.now(), "Summary");
        article.setId(10L);
        article.setArticleScore(13);
        article.setImportanceLabel("Must Read");
        Keyword keyword = new Keyword(1L, "Java", "java", "Java", "Core", 5, 3, 2, 0, 5, 0, 10, null, Instant.now());
        WeeklyReport report = new WeeklyReport(LocalDate.now().minusDays(6), LocalDate.now(),
                List.of(article), List.of(keyword), Map.of());

        String markdown = new MarkdownReportWriter().render(report);

        assertTrue(markdown.contains("### 1. Java architecture"));
        assertTrue(markdown.contains("| Java | Core | 3 |"));
    }
}
