package com.example.techwatch.score;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordExtractor;
import com.example.techwatch.source.Source;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordBasedArticleScorerTest {
    @Test
    void scoresTitleSummaryTrustAndCoreBonus() {
        Article article = Article.fetched(1L, "Blog", "JAVA patterns", "https://example.com/a", Instant.now(), "Run with Docker");
        Keyword java = new Keyword("Java", "Java", "Core", 5);
        Keyword docker = new Keyword("Docker", "Infrastructure", "Core", 4);
        var matches = new KeywordExtractor().extract(article, List.of(java, docker));

        var score = new KeywordBasedArticleScorer().score(article,
                new Source(1L, "Blog", "https://example.com", "rss", 5, "ACTIVE"), matches);

        assertEquals(List.of("Java", "Docker"), score.matchedKeywords());
        assertTrue(score.score() >= 12);
        assertEquals("Must Read", score.label());
    }

    @Test
    void buzzOnlyGetsPenalty() {
        Article article = Article.fetched(1L, "Blog", "AI Agent launch", "https://example.com/b", Instant.now(), "");
        Keyword buzz = new Keyword("AI Agent", "AI", "Buzz", 4);
        var score = new KeywordBasedArticleScorer().score(article,
                new Source(1L, "Blog", "https://example.com", "rss", 1, "ACTIVE"),
                new KeywordExtractor().extract(article, List.of(buzz)));
        assertEquals(3.0, score.score());
    }
}
