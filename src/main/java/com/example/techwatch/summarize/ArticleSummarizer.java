package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;

public interface ArticleSummarizer {
    ArticleSummary summarize(Article article, String bodyText);
}
