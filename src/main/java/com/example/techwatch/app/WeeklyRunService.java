package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.body.BodyExtractor;
import com.example.techwatch.body.SimpleBodyExtractor;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.KeywordConfigLoader;
import com.example.techwatch.config.RetentionConfigLoader;
import com.example.techwatch.config.SourceConfigLoader;
import com.example.techwatch.db.ArticleRepository;
import com.example.techwatch.db.ArticleBodyRepository;
import com.example.techwatch.db.ArticleSummaryRepository;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordMentionRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.ReportRepository;
import com.example.techwatch.db.SourceRepository;
import com.example.techwatch.db.UserProfileRepository;
import com.example.techwatch.db.DiscoveredKeywordRepository;
import com.example.techwatch.db.JobMarketSnapshotRepository;
import com.example.techwatch.db.KeywordMarketStatsRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.fetch.FeedFetcher;
import com.example.techwatch.fetch.RssFeedFetcher;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordEvaluator;
import com.example.techwatch.keyword.KeywordExtractor;
import com.example.techwatch.keyword.KeywordTrendEvaluator;
import com.example.techwatch.explore.DiscoveredKeyword;
import com.example.techwatch.explore.ExploreService;
import com.example.techwatch.market.KeywordMarketEvaluator;
import com.example.techwatch.market.KeywordMarketStats;
import com.example.techwatch.market.ManualCsvJobMarketSource;
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
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        log.accept("情報源を読み込みました: " + configuredSources.size() + "件");
        log.accept("キーワードを読み込みました: " + configuredKeywords.size() + "件");

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
        Set<String> interests = new UserProfileRepository(database).findEnabledInterests();
        ArticleService articleService = new ArticleService(articleRepository, new ArticleBodyRepository(database),
                keywordRepository, mentionRepository,
                summaryRepository, feedFetcher, new KeywordExtractor(), new KeywordBasedArticleScorer(interests),
                bodyExtractor, summarizer);
        ArticleRunStats stats = articleService.collect(sources, keywords, log);
        log.accept("取得した記事: " + stats.fetched() + "件");
        log.accept("新しく保存した記事: " + stats.saved() + "件");
        log.accept("重複でスキップした記事: " + stats.duplicates() + "件");
        log.accept("評価した記事: " + stats.scored() + "件");
        log.accept("検出したキーワード言及: " + stats.keywordMentions() + "件");

        WeeklyPeriod period = WeeklyPeriod.previousCompleted(LocalDate.now(APP_ZONE));
        LocalDate startDate = period.start();
        LocalDate endDate = period.end();
        Instant start = startDate.atStartOfDay(APP_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        Map<Long, Source> sourceById = sources.stream().collect(Collectors.toMap(Source::id, Function.identity()));
        int rescored = articleService.rescore(articleRepository.findBetween(start, end), keywords, sourceById, log);
        log.accept("現在の学習設定で今週の記事を再評価しました: " + rescored + "件");
        KeywordService keywordService = new KeywordService(keywordRepository, mentionRepository, new KeywordEvaluator());
        keywordService.evaluate(start, end);

        LocalDate weekStart = startDate;
        KeywordWeeklyStatsRepository weeklyStatsRepository = new KeywordWeeklyStatsRepository(database);
        if (!weeklyStatsRepository.hasAnyStats()) {
            int backfilled = weeklyStatsRepository.backfillHistoricalWeeks(
                    weekStart, 52, configuredSources.size());
            log.accept("RSSから取得できた過去記事を最大52週まで自動集計しました: "
                    + backfilled + "週（履歴部分取得）");
        }
        KeywordHistoryService historyService = new KeywordHistoryService(keywordRepository,
                weeklyStatsRepository, new KeywordTrendEvaluator());
        int successfulSources = Math.max(0, configuredSources.size() - stats.failedSources());
        keywords = historyService.captureAndEvaluate(weekStart, successfulSources, configuredSources.size());
        log.accept("取得品質と言及率を含む過去104週の履歴から最近の動きを評価しました");

        JobMarketService jobMarketService = new JobMarketService(new ManualCsvJobMarketSource(),
                new JobMarketSnapshotRepository(database), new KeywordMarketStatsRepository(database),
                keywordRepository, new KeywordMarketEvaluator());
        Map<Long, KeywordMarketStats> marketStats = jobMarketService.refresh(paths.jobMarketCsv(), keywords, weekStart);
        keywords = new KeywordPriorityService(keywordRepository).evaluate(keywords, marketStats);
        log.accept("求人市場CSVを評価しました: " + marketStats.values().stream()
                .filter(value -> value.usJobCount() > 0 || value.jpJobCount() > 0).count() + "キーワード");

        List<Article> currentArticles = articleRepository.findBetween(start, end);
        Map<Long, com.example.techwatch.summarize.ArticleSummary> currentSummaries = summaryRepository.findAll();
        List<DiscoveredKeyword> discovered = new ExploreService(new DiscoveredKeywordRepository(database))
                .discover(currentArticles, currentSummaries, keywords);
        log.accept("探索中の未知キーワード: " + discovered.size() + "件");

        ReportService reportService = new ReportService(articleRepository, summaryRepository, keywordRepository,
                new ReportRepository(database), new MarkdownReportWriter());
        ReportService.ReportOutput report = reportService.generate(startDate, endDate, start, end,
                paths.reportsDirectory(), discovered, marketStats);
        weeklyStatsRepository.refreshReportIncludedCounts(weekStart);
        log.accept("週報を生成しました: " + report.path());
        try {
            CleanupResult cleanup = new CleanupService(database, paths,
                    new RetentionConfigLoader().load(paths.retentionConfig())).cleanup();
            log.accept(cleanup.summaryJapanese());
        } catch (Exception error) {
            log.accept("古いデータの整理をスキップしました: " + error.getMessage());
        }
        if (stats.failedSources() > 0 || stats.failedArticles() > 0) {
            log.accept("一部失敗: 情報源=" + stats.failedSources() + "件、記事=" + stats.failedArticles() + "件");
        }
        writeExecutionLog(logs);
        return new WeeklyRunResult(report.path(), report.markdown(), report.articles(), report.keywords(),
                report.summaries(), logs, stats, discovered, marketStats);
    }

    private void writeExecutionLog(List<String> logs) {
        try {
            String timestamp = Instant.now().toString().replace(':', '-');
            Files.writeString(paths.logsDirectory().resolve("run-" + timestamp + ".log"), String.join("\n", logs));
        } catch (Exception ignored) {
            // ログ保存の失敗だけで週報生成を失敗させない。
        }
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
        return new WeeklyRunResult(latest, markdown, articles, keywords,
                new ArticleSummaryRepository(database).findAll(), List.of(), null,
                new DiscoveredKeywordRepository(database).findAllActive(),
                new KeywordMarketStatsRepository(database).findLatestByKeyword());
    }
}
