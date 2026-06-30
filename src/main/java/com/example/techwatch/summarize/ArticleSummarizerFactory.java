package com.example.techwatch.summarize;

public final class ArticleSummarizerFactory {
    private ArticleSummarizerFactory() { }

    public static ArticleSummarizer fromEnvironment() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) return new NoopArticleSummarizer();
        String model = System.getenv("OPENAI_MODEL");
        return new OpenAiArticleSummarizer(key, model);
    }
}
