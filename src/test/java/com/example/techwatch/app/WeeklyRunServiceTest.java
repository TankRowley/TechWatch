package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.body.BodyExtractionResult;
import com.example.techwatch.body.BodyStatus;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.fetch.FeedFetchResult;
import com.example.techwatch.source.Source;
import com.example.techwatch.summarize.NoopArticleSummarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyRunServiceTest {
    @TempDir Path temp;

    @Test
    void runsCompletePipelineWithoutExternalNetwork() throws Exception {
        Files.writeString(temp.resolve("sources.yml"), "sources:\n  - { name: Test, type: rss, url: 'https://example.com/feed', trustScore: 5 }\n");
        Files.writeString(temp.resolve("keywords.yml"), "keywords:\n  - { name: Java, category: Java, status: Core, weight: 5 }\n");
        var fetcher = (com.example.techwatch.fetch.FeedFetcher) source -> FeedFetchResult.success(java.util.List.of(
                Article.fetched(source.id(), source.name(), "Java architecture", "https://example.com/article",
                        previousCompletedWeekInstant(), "A practical Java implementation")));
        WeeklyRunService service = new WeeklyRunService(new AppPaths(temp), fetcher,
                url -> new BodyExtractionResult(BodyStatus.SUCCESS, "A sufficiently detailed Java article body",
                        "<html><body>raw source</body></html>", ""), new NoopArticleSummarizer());

        WeeklyRunResult result = service.runWeekly(line -> { });

        assertEquals(1, result.stats().saved());
        assertTrue(Files.exists(result.reportPath()));
        assertTrue(result.reportMarkdown().contains("Java architecture"));
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + temp.resolve("techwatch.db"));
             var statement = connection.createStatement();
             var body = statement.executeQuery("SELECT body_text,raw_html FROM article_bodies")) {
            assertTrue(body.next());
            assertTrue(body.getString("body_text").contains("Java article body"));
            assertTrue(body.getString("raw_html").contains("raw source"));
        }
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + temp.resolve("techwatch.db"));
             var statement = connection.createStatement();
             var stats = statement.executeQuery("SELECT report_included_count,total_article_count,collection_status FROM keyword_weekly_stats")) {
            assertTrue(stats.next());
            assertEquals(1, stats.getInt(1));
            assertEquals(1, stats.getInt(2));
            assertEquals("SUCCESS", stats.getString(3));
        }
    }

    @Test
    void retriesArticleWhoseProcessingFailed() throws Exception {
        Files.writeString(temp.resolve("sources.yml"), "sources:\n  - { name: Test, type: rss, url: 'https://example.com/feed', trustScore: 5 }\n");
        Files.writeString(temp.resolve("keywords.yml"), "keywords:\n  - { name: Java, category: Java, status: Core, weight: 5 }\n");
        var fetcher = (com.example.techwatch.fetch.FeedFetcher) source -> FeedFetchResult.success(List.of(
                Article.fetched(source.id(), source.name(), "Java retry", "https://example.com/retry",
                        Instant.now(), "A practical Java implementation")));
        AtomicInteger attempts = new AtomicInteger();
        var summarizer = (com.example.techwatch.summarize.ArticleSummarizer) (article, body) -> {
            if (attempts.getAndIncrement() == 0) throw new IllegalStateException("temporary failure");
            return new com.example.techwatch.summarize.ArticleSummary("再試行成功", List.of(), "", "Soon",
                    List.of(), List.of("Java"), article.getImportanceLabel());
        };
        WeeklyRunService service = new WeeklyRunService(new AppPaths(temp), fetcher,
                url -> new BodyExtractionResult(BodyStatus.SUCCESS, "A sufficiently detailed Java article body",
                        "<html><body>raw source</body></html>", ""), summarizer);

        WeeklyRunResult first = service.runWeekly(line -> { });
        WeeklyRunResult second = service.runWeekly(line -> { });

        assertEquals(1, first.stats().failedArticles());
        assertEquals(1, second.stats().scored());
        assertEquals("再試行成功", second.summaries().values().iterator().next().shortSummary());
    }
    private Instant previousCompletedWeekInstant() {
        LocalDate day=WeeklyPeriod.previousCompleted(LocalDate.now(ZoneId.of("Asia/Tokyo"))).start();
        return day.atTime(12,0).atZone(ZoneId.of("Asia/Tokyo")).toInstant();
    }
}
