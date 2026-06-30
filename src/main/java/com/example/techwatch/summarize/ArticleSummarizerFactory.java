package com.example.techwatch.summarize;

public final class ArticleSummarizerFactory {
    private ArticleSummarizerFactory() { }

    public static ArticleSummarizer fromEnvironment() {
        String key = System.getenv("OPENAI_API_KEY");
        String baseUrl = System.getenv("OPENAI_BASE_URL");
        if ((key == null || key.isBlank()) && (baseUrl == null || baseUrl.isBlank())) {
            return new NoopArticleSummarizer();
        }
        String model = System.getenv("OPENAI_MODEL");
        return new OpenAiArticleSummarizer(key, model, baseUrl);
    }
}
