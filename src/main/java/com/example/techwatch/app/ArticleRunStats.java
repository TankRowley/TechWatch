package com.example.techwatch.app;

public record ArticleRunStats(int fetched, int saved, int duplicates, int scored,
                              int keywordMentions, int failedArticles, int failedSources) {
}
