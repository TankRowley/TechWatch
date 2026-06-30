package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.body.BodyExtractor;
import com.example.techwatch.body.SimpleBodyExtractor;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.KeywordConfigLoader;
import com.example.techwatch.config.SourceConfigLoader;
import com.example.techwatch.db.ArticleRepository;
import com.example.techwatch.db.ArticleSummaryRepository;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordMentionRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.ReportRepository;
import com.example.techwatch.db.SourceRepository;
import com.example.techwatch.fetch.FeedFetcher;
import com.example.techwatch.fetch.RssFeedFetcher;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordEvaluator;
import com.example.techwatch.keyword.KeywordExtractor;
import com.example.techwatch.report.MarkdownReportWriter;
import com.example.techwatch.score.KeywordBasedArticleScorer;
import com.example.techwatch.source.Source;
import com.example.techwatch.summarize.ArticleSummarizer;
import com.example.techwatch.summarize.ArticleSummarizerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WeeklyRunService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Tokyo");
    private final AppPaths paths;
    private final FeedFetcher feedFetcher;
    private final BodyExtractor bodyExtractor;
    private final ArticleSummarizer summarizer;

    public WeeklyRunService() {
        this(AppPaths.detect(), new RssFeedFetcher(), new SimpleBodyExtractor(), ArticleSummarizerFactory.fromEnvironment());
    }

    public WeeklyRunService(AppPaths paths, FeedFetcher feedFetcher, BodyExtractor bodyExtractor,
                            ArticleSummarizer summarizer) {
        this.paths = paths;
        this.feedFetcher = feedFetcher;
        this.bodyExtractor = bodyExtractor;
        this.summarizer = summarizer;
    }

    public WeeklyRunResult runWeekly() throws Exception { return runWeekly(System.out::println); }

    public WeeklyRunResult runWeekly(Consumer<String> output) throws Exception {
        List<String> logs = new ArrayList<>();
        Consumer<String> log = line -> { logs.add(line); output.accept(line); };
        paths.ensureDirectories();
        List<Source> configuredSources = new SourceConfigLoader().load(paths.sourceConfig());
        List<Keyword> configuredKeywords = new KeywordConfigLoader().load(paths.keywordConfig());
        log.accept("Loaded sources: " + configuredSources.size());
        log.accept("Loaded keywords: " + configuredKeywords.size());

        Database database = new Database(paths.database());
        database.initialize();
        SourceRepository sourceRepository = new SourceRepository(database);
        KeywordRepository keywordRepository = new KeywordRepository(database);
        List<Source> sources = new ArrayList<>();
        for (Source source : configuredSources) sources.add(sourceRepository.save(source));
        List<Keyword> keywords = new ArrayList<>();
        for (Keyword keyword : configuredKeywords) keywords.add(keywordRepository.save(keyword));

        ArticleRepository articleRepository = new ArticleRepository(database);
        KeywordMentionRepository mentionRepository = new KeywordMentionRepository(database);
        ArticleSummaryRepository summaryRepository = new ArticleSummaryRepository(database);
        ArticleService articleService = new ArticleService(articleRepository, keywordRepository, mentionRepository,
                summaryRepository, feedFetcher, new KeywordExtractor(), new KeywordBasedArticleScorer(),
                bodyExtractor, summarizer);
        ArticleRunStats stats = articleService.collect(sources, keywords, log);
        log.accept("Fetched articles: " + stats.fetched());
        log.accept("Saved new articles: " + stats.saved());
        log.accept("Skipped duplicates: " + stats.duplicates());
        log.accept("Scored articles: " + stats.scored());
        log.accept("Keyword mentions: " + stats.keywordMentions());

        LocalDate endDate = LocalDate.now(APP_ZONE);
        LocalDate startDate = endDate.minusDays(6);
        Instant start = startDate.atStartOfDay(APP_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        KeywordService keywordService = new KeywordService(keywordRepository, mentionRepository, new KeywordEvaluator());
        keywordService.evaluate(start, end);

        ReportService reportService = new ReportService(articleRepository, summaryRepository, keywordRepository,
                new ReportRepository(database), new MarkdownReportWriter());
        ReportService.ReportOutput report = reportService.generate(startDate, endDate, start, end, paths.reportsDirectory());
        log.accept("Report generated: " + report.path());
        if (stats.failedSources() > 0 || stats.failedArticles() > 0) {
            log.accept("Partial failures: sources=" + stats.failedSources() + ", articles=" + stats.failedArticles());
        }
        return new WeeklyRunResult(report.path(), report.markdown(), report.articles(), report.keywords(), logs, stats);
    }

    public WeeklyRunResult loadLatest() throws Exception {
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();
        List<Article> articles = new ArticleRepository(database).findAllByScore();
        List<Keyword> keywords = new KeywordRepository(database).findAll();
        Path latest = null;
        try (var files = Files.list(paths.reportsDirectory())) {
            latest = files.filter(path -> path.getFileName().toString().endsWith(".md")).sorted().reduce((a, b) -> b).orElse(null);
        }
        String markdown = latest == null ? "週報はまだありません。「週報を生成」を押してください。" : Files.readString(latest);
        return new WeeklyRunResult(latest, markdown, articles, keywords, List.of(), null);
    }
}
