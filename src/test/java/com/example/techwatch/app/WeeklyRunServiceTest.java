package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.body.BodyExtractionResult;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.fetch.FeedFetchResult;
import com.example.techwatch.source.Source;
import com.example.techwatch.summarize.NoopArticleSummarizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

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
                        Instant.now(), "A practical Java implementation")));
        WeeklyRunService service = new WeeklyRunService(new AppPaths(temp), fetcher,
                url -> BodyExtractionResult.skipped(), new NoopArticleSummarizer());

        WeeklyRunResult result = service.runWeekly(line -> { });

        assertEquals(1, result.stats().saved());
        assertTrue(Files.exists(result.reportPath()));
        assertTrue(result.reportMarkdown().contains("Java architecture"));
    }
}
