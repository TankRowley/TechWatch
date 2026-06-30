package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LmStudioIntegrationTest {
    @Test
    @EnabledIfSystemProperty(named = "techwatch.lmstudio.test", matches = "true")
    void summarizesThroughLmStudioOpenAiCompatibilityApi() {
        String model = System.getProperty("techwatch.lmstudio.model", "techwatch-local");
        Article article = Article.fetched(1L, "Local Test", "Java 21 Virtual Threads",
                "https://example.com/java21", Instant.now(), "test");
        ArticleSummarizer summarizer = new OpenAiArticleSummarizer("", model, "http://localhost:1234/v1");

        ArticleSummary result = summarizer.summarize(article, """
                Virtual threads are lightweight threads designed for high-throughput concurrent applications.
                They simplify blocking I/O code while preserving the familiar thread-per-request style.
                """);

        assertFalse(result.shortSummary().isBlank());
        assertFalse(result.shortSummary().startsWith("日本語要約はまだ"));
        assertTrue(Set.of("Must Read", "Watch", "Skim", "Archive", "Ignore")
                .contains(result.importanceLabel()));
    }
}
