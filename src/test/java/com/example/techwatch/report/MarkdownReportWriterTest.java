package com.example.techwatch.report;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;
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
        Keyword keyword = new Keyword(1L, "Java", "java", "Java", "Core", 5, 3, 2, 0, 5, 0, 10,
                null, Instant.now(), true, Instant.now(), "Javaバックエンドを追うため",
                true, Instant.now(), "現在学習中");
        WeeklyReport report = new WeeklyReport(LocalDate.now().minusDays(6), LocalDate.now(),
                List.of(article), List.of(keyword), Map.of(10L, new ArticleSummary("Java設計の記事です。",
                List.of("設計"), "設計学習に役立つため。", "Soon", List.of("Java"), List.of("Java"), "Must Read")));

        String markdown = new MarkdownReportWriter().render(report);

        assertTrue(markdown.contains("### 1. Java architecture"));
        assertTrue(markdown.contains("| Java | 基礎 | 3 |"));
        assertTrue(markdown.contains("## 5. 固定キーワードの動き"));
        assertTrue(markdown.contains("## 6. 学習中キーワードに関係する記事"));
        assertTrue(markdown.contains("Javaバックエンドを追うため"));
        assertTrue(markdown.contains("概要:\nJava設計の記事です。"));
    }
}
