package com.example.techwatch.fetch;

import com.example.techwatch.article.Article;

import java.util.List;

public record FeedFetchResult(List<Article> articles, String errorMessage) {
    public FeedFetchResult {
        articles = articles == null ? List.of() : List.copyOf(articles);
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static FeedFetchResult success(List<Article> articles) { return new FeedFetchResult(articles, ""); }
    public static FeedFetchResult failure(String message) { return new FeedFetchResult(List.of(), message); }
    public boolean successful() { return errorMessage.isBlank(); }
}
