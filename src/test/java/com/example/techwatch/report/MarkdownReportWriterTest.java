package com.example.techwatch.report;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;
import com.example.techwatch.explore.DiscoveredKeyword;
import com.example.techwatch.market.KeywordMarketStats;
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
                List.of("設計"), "設計学習に役立つため。", "Soon", List.of("Java"), List.of("Java"), "Must Read")),
                List.of(new DiscoveredKeyword(1L, "OpenTelemetry", "opentelemetry", "DevOps",
                        "ログやトレースを標準化する仕組み。", "LATER", List.of("ログ"), Instant.now(),
                        Instant.now(), 2, false)),
                Map.of(1L, new KeywordMarketStats(1L, LocalDate.now(), 12000, 8500, 0, 0, 0, 0,
                        70, 68, 75, "Stable Demand")));

        String markdown = new MarkdownReportWriter().render(report);

        assertTrue(markdown.contains("### 1. Java architecture"));
        assertTrue(markdown.contains("| Java | 基礎 | 休眠 | 3 |"));
        assertTrue(markdown.contains("## 5. 固定キーワードの動き"));
        assertTrue(markdown.contains("## 6. 学習中キーワードに関係する記事"));
        assertTrue(markdown.contains("Javaバックエンドを追うため"));
        assertTrue(markdown.contains("概要:\nJava設計の記事です。"));
        assertTrue(markdown.contains("## 12. 求人市場シグナル"));
        assertTrue(markdown.contains("市場評価: 安定需要"));
        assertTrue(markdown.contains("## 13. 今週の未知キーワード"));
        assertTrue(markdown.contains("判断: 後で学ぶ"));
        assertTrue(markdown.contains("## 14. 減速中・休眠中のキーワード"));
    }
}
