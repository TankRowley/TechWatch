package com.example.techwatch.summarize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
