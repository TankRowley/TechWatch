package com.example.techwatch.mail;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.article.Article;
import com.example.techwatch.summarize.ArticleSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailComposeServiceTest {
    @Test
    void createsPrefilledGmailDraftWithSummaryAndLink() {
        Article article = Article.fetched(1L, "Example", "Javaの新機能", "https://example.com/java",
                Instant.now(), "original");
        article.setId(10L);
        article.setImportanceLabel("Must Read");
        ArticleSummary summary = new ArticleSummary("Javaの重要な更新です。", List.of(),
                "現在の学習内容に直結するため。", "Now", List.of(), List.of("Java"), "Must Read");
        WeeklyRunResult result = new WeeklyRunResult(null, "", List.of(article), List.of(),
                Map.of(10L, summary), List.of(), null);

        GmailComposeService.GmailDraft draft = new GmailComposeService().create("reader@gmail.com", result);

        assertTrue(draft.subject().contains("必読1件"));
        assertTrue(draft.body().contains("Javaの重要な更新です。"));
        assertTrue(draft.body().contains("https://example.com/java"));
        assertTrue(draft.composeUrl().startsWith("https://mail.google.com/mail/?"));
        assertTrue(draft.composeUrl().contains("to=reader%40gmail.com"));
    }
}
