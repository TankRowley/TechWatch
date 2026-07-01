package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.article.ArticleScore;
import com.example.techwatch.body.BodyExtractionResult;
import com.example.techwatch.body.BodyExtractor;
import com.example.techwatch.db.ArticleRepository;
import com.example.techwatch.db.ArticleBodyRepository;
import com.example.techwatch.db.ArticleSummaryRepository;
import com.example.techwatch.db.KeywordMentionRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.fetch.FeedFetchResult;
import com.example.techwatch.fetch.FeedFetcher;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordExtractor;
import com.example.techwatch.keyword.KeywordMatch;
import com.example.techwatch.score.ArticleScorer;
import com.example.techwatch.source.Source;
import com.example.techwatch.summarize.ArticleSummarizer;
import com.example.techwatch.summarize.ArticleSummary;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.Map;

public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleBodyRepository bodyRepository;
    private final KeywordRepository keywordRepository;
    private final KeywordMentionRepository mentionRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final FeedFetcher feedFetcher;
    private final KeywordExtractor keywordExtractor;
    private final ArticleScorer articleScorer;
    private final BodyExtractor bodyExtractor;
    private final ArticleSummarizer summarizer;

    public ArticleService(ArticleRepository articleRepository, ArticleBodyRepository bodyRepository,
                          KeywordRepository keywordRepository,
                          KeywordMentionRepository mentionRepository, ArticleSummaryRepository summaryRepository,
                          FeedFetcher feedFetcher, KeywordExtractor keywordExtractor, ArticleScorer articleScorer,
                          BodyExtractor bodyExtractor, ArticleSummarizer summarizer) {
        this.articleRepository = articleRepository;
        this.bodyRepository = bodyRepository;
        this.keywordRepository = keywordRepository;
        this.mentionRepository = mentionRepository;
        this.summaryRepository = summaryRepository;
        this.feedFetcher = feedFetcher;
        this.keywordExtractor = keywordExtractor;
        this.articleScorer = articleScorer;
        this.bodyExtractor = bodyExtractor;
        this.summarizer = summarizer;
    }

    public ArticleRunStats collect(List<Source> sources, List<Keyword> keywords, Consumer<String> log) {
        int fetched = 0, saved = 0, duplicates = 0, scored = 0, mentions = 0, failedArticles = 0, failedSources = 0;
        for (Source source : sources) {
            FeedFetchResult fetchResult = feedFetcher.fetch(source);
            if (!fetchResult.successful()) {
                failedSources++;
                log.accept("情報源の取得に失敗: " + source.name() + "（" + fetchResult.errorMessage() + "）");
                continue;
            }
            fetched += fetchResult.articles().size();
            for (Article article : fetchResult.articles()) {
                try {
                    article.setSourceId(source.id());
                    article.setSourceName(source.name());
                    Optional<Article> inserted = articleRepository.save(article);
                    Article persisted;
                    if (inserted.isEmpty()) {
                        if (articleRepository.isProcessingComplete(article.getUrl())) { duplicates++; continue; }
                        persisted = articleRepository.findByUrl(article.getUrl()).orElseThrow();
                        log.accept("未完了の記事を再処理します: " + persisted.getTitle());
                    } else {
                        saved++;
                        persisted = inserted.get();
                    }
                    articleRepository.markProcessing(persisted.getId());
                    List<KeywordMatch> matches = keywordExtractor.extract(persisted, keywords);
                    ArticleScore score = articleScorer.score(persisted, source, matches);
                    persisted.setArticleScore(score.score());
                    persisted.setImportanceLabel(score.label());

                    BodyExtractionResult body = shouldFetchBody(score) ? bodyExtractor.extract(persisted.getUrl())
                            : BodyExtractionResult.skipped();
                    persisted.setBodyStatus(body.status().name());
                    articleRepository.updateAnalysis(persisted.getId(), score.score(), score.label(), body.status().name());
                    bodyRepository.save(persisted.getId(), body.bodyText(), body.rawHtml(), persisted.getFetchedAt());
                    ArticleSummary summary = summarizer.summarize(persisted,
                            body.bodyText().isBlank() ? persisted.getSummaryOriginal() : body.bodyText());
                    summaryRepository.save(persisted.getId(), summary);
                    scored++;

                    Instant observedAt = persisted.getPublishedAt() == null ? persisted.getFetchedAt() : persisted.getPublishedAt();
                    for (KeywordMatch match : matches) {
                        Keyword keyword = match.keyword();
                        for (String detectedIn : match.detectedIn()) {
                            if (mentionRepository.saveMention(persisted.getId(), keyword.getId(), detectedIn, observedAt)) mentions++;
                        }
                        keywordRepository.updateSeenAt(keyword.getId(), observedAt);
                    }
                    articleRepository.markProcessingComplete(persisted.getId());
                } catch (Exception error) {
                    failedArticles++;
                    try {
                        articleRepository.findByUrl(article.getUrl()).ifPresent(persisted -> {
                            try { articleRepository.markProcessingFailed(persisted.getId(), error.toString()); }
                            catch (Exception ignored) { }
                        });
                    } catch (Exception ignored) { }
                    log.accept("記事の処理に失敗: " + article.getTitle() + "（" + error.getMessage() + "）");
                }
            }
        }
        return new ArticleRunStats(fetched, saved, duplicates, scored, mentions, failedArticles, failedSources);
    }

    private boolean shouldFetchBody(ArticleScore score) {
        String skip = System.getenv("TECHWATCH_SKIP_BODY");
        return !"true".equalsIgnoreCase(skip) && score.score() >= 5;
    }

    public int rescore(List<Article> articles, List<Keyword> keywords, Map<Long, Source> sources,
                       Consumer<String> log) {
        int updated = 0;
        for (Article article : articles) {
            try {
                Source source = sources.get(article.getSourceId());
                if (source == null) continue;
                ArticleScore score = articleScorer.score(article, source, keywordExtractor.extract(article, keywords));
                articleRepository.updateAnalysis(article.getId(), score.score(), score.label(), article.getBodyStatus());
                article.setArticleScore(score.score());
                article.setImportanceLabel(score.label());
                updated++;
            } catch (Exception error) {
                log.accept("記事の再評価に失敗: " + article.getTitle() + "（" + error.getMessage() + "）");
            }
        }
        return updated;
    }
}
