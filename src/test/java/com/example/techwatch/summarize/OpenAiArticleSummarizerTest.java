package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void acceptsLmStudioBaseUrlAndFullResponsesEndpoint() {
        assertEquals("http://localhost:1234/v1/responses",
                OpenAiArticleSummarizer.responsesEndpoint("http://localhost:1234/v1").toString());
        assertEquals("http://localhost:1234/v1/responses",
                OpenAiArticleSummarizer.responsesEndpoint("http://localhost:1234/v1/responses/").toString());
        assertEquals("http://localhost:1234/v1/chat/completions",
                OpenAiArticleSummarizer.chatCompletionsEndpoint("http://localhost:1234/v1/").toString());
    }

    @Test
    void rejectsUnsupportedEndpointScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenAiArticleSummarizer.responsesEndpoint("file:///tmp/local-ai"));
    }

    @Test
    void parsesLmStudioReasoningContent() throws Exception {
        String json = """
                {"summary":"仮想スレッドの概要","technicalPoints":["軽量スレッド"],
                "whyItMatters":"並行I/Oを簡潔に扱える","learningPriority":"Now",
                "prerequisites":["Thread"],"keywords":["Virtual Threads"],"importanceLabel":"Must Read"}
                """;
        String response = """
                {"choices":[{"message":{"role":"assistant","content":"","reasoning_content":%s}}]}
                """.formatted(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(json));

        ArticleSummary result = new OpenAiArticleSummarizer("", "techwatch-local",
                "http://localhost:1234/v1").parseChatResponse(response);

        assertEquals("仮想スレッドの概要", result.shortSummary());
        assertEquals("Must Read", result.importanceLabel());
    }

    @Test
    void localRequestUsesChatCompletionsStructuredOutput() throws Exception {
        Article article = Article.fetched(1L, "Blog", "Java 21", "https://example.com", Instant.now(), "summary");
        String request = new OpenAiArticleSummarizer("", "techwatch-local",
                "http://localhost:1234/v1").buildChatRequest(article, "Virtual threads");

        assertTrue(request.contains("response_format"));
        assertTrue(request.contains("json_schema"));
        assertTrue(request.contains("short Japanese values"));
    }
}
