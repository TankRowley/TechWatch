package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.article.ArticleScore;
import com.example.techwatch.body.BodyExtractionResult;
import com.example.techwatch.body.BodyExtractor;
import com.example.techwatch.db.ArticleRepository;
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

public class ArticleService {
    private final ArticleRepository articleRepository;
    private final KeywordRepository keywordRepository;
    private final KeywordMentionRepository mentionRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final FeedFetcher feedFetcher;
    private final KeywordExtractor keywordExtractor;
    private final ArticleScorer articleScorer;
    private final BodyExtractor bodyExtractor;
    private final ArticleSummarizer summarizer;

    public ArticleService(ArticleRepository articleRepository, KeywordRepository keywordRepository,
                          KeywordMentionRepository mentionRepository, ArticleSummaryRepository summaryRepository,
                          FeedFetcher feedFetcher, KeywordExtractor keywordExtractor, ArticleScorer articleScorer,
                          BodyExtractor bodyExtractor, ArticleSummarizer summarizer) {
        this.articleRepository = articleRepository;
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
                log.accept("Source failed: " + source.name() + " (" + fetchResult.errorMessage() + ")");
                continue;
            }
            fetched += fetchResult.articles().size();
            for (Article article : fetchResult.articles()) {
                try {
                    article.setSourceId(source.id());
                    article.setSourceName(source.name());
                    Optional<Article> inserted = articleRepository.save(article);
                    if (inserted.isEmpty()) { duplicates++; continue; }
                    saved++;
                    Article persisted = inserted.get();
                    List<KeywordMatch> matches = keywordExtractor.extract(persisted, keywords);
                    ArticleScore score = articleScorer.score(persisted, source, matches);
                    persisted.setArticleScore(score.score());
                    persisted.setImportanceLabel(score.label());

                    BodyExtractionResult body = shouldFetchBody(score) ? bodyExtractor.extract(persisted.getUrl())
                            : BodyExtractionResult.skipped();
                    persisted.setBodyStatus(body.status().name());
                    articleRepository.updateAnalysis(persisted.getId(), score.score(), score.label(), body.status().name());
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
                } catch (Exception error) {
                    failedArticles++;
                    log.accept("Article failed: " + article.getTitle() + " (" + error.getMessage() + ")");
                }
            }
        }
        return new ArticleRunStats(fetched, saved, duplicates, scored, mentions, failedArticles, failedSources);
    }

    private boolean shouldFetchBody(ArticleScore score) {
        String skip = System.getenv("TECHWATCH_SKIP_BODY");
        return !"true".equalsIgnoreCase(skip) && score.score() >= 5;
    }
}
