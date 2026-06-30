package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiArticleSummarizerTest {
    @Test
    void parsesStructuredOutputWithoutApiCall() throws Exception {
        String summary = """
                {"summary":"三行要約","technicalPoints":["Docker"],"whyItMatters":"基盤技術との接点",
                "learningPriority":"Soon","prerequisites":["HTTP"],"keywords":["Docker"],"importanceLabel":"Watch"}
                """;
        String response = """
                {"output":[{"type":"message","content":[{"type":"output_text","text":%s}]}]}
                """.formatted(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(summary));

        ArticleSummary result = new OpenAiArticleSummarizer("dummy", "gpt-5-mini").parseResponse(response);

        assertEquals("三行要約", result.shortSummary());
        assertEquals("Soon", result.learningPriority());
    }

    @Test
    void promptRequiresJapaneseValues() throws Exception {
        Article article = Article.fetched(1L, "Blog", "English title", "https://example.com", Instant.now(), "summary");
        String request = new OpenAiArticleSummarizer("dummy", "gpt-5-mini").buildRequest(article, "English body");
        assertTrue(request.contains("出力は必ず日本語にしてください"));
        assertTrue(request.contains("英語記事であっても日本語で要約"));
    }
}
