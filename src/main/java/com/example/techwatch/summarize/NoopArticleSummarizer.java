package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;

import java.util.List;

public class NoopArticleSummarizer implements ArticleSummarizer {
    @Override
    public ArticleSummary summarize(Article article, String bodyText) {
        String summary = "日本語要約はまだ生成されていません。OPENAI_API_KEYを設定すると、英語記事も日本語で要約されます。";
        return new ArticleSummary(summary, List.of(), "", "Watch Only", List.of(), List.of(),
                article.getImportanceLabel());
    }
}
