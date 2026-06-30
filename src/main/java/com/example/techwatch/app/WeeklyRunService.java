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
import com.example.techwatch.db.UserProfileRepository;
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
        ArticleService articleService = new ArticleService(articleRepository, keywordRepository, mentionRepository,
                summaryRepository, feedFetcher, new KeywordExtractor(), new KeywordBasedArticleScorer(interests),
                bodyExtractor, summarizer);
        ArticleRunStats stats = articleService.collect(sources, keywords, log);
        log.accept("取得した記事: " + stats.fetched() + "件");
        log.accept("新しく保存した記事: " + stats.saved() + "件");
        log.accept("重複でスキップした記事: " + stats.duplicates() + "件");
        log.accept("評価した記事: " + stats.scored() + "件");
        log.accept("検出したキーワード言及: " + stats.keywordMentions() + "件");

        LocalDate endDate = LocalDate.now(APP_ZONE);
        LocalDate startDate = endDate.minusDays(6);
        Instant start = startDate.atStartOfDay(APP_ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        Map<Long, Source> sourceById = sources.stream().collect(Collectors.toMap(Source::id, Function.identity()));
        int rescored = articleService.rescore(articleRepository.findBetween(start, end), keywords, sourceById, log);
        log.accept("現在の学習設定で今週の記事を再評価しました: " + rescored + "件");
        KeywordService keywordService = new KeywordService(keywordRepository, mentionRepository, new KeywordEvaluator());
        keywordService.evaluate(start, end);

        ReportService reportService = new ReportService(articleRepository, summaryRepository, keywordRepository,
                new ReportRepository(database), new MarkdownReportWriter());
        ReportService.ReportOutput report = reportService.generate(startDate, endDate, start, end, paths.reportsDirectory());
        log.accept("週報を生成しました: " + report.path());
        if (stats.failedSources() > 0 || stats.failedArticles() > 0) {
            log.accept("一部失敗: 情報源=" + stats.failedSources() + "件、記事=" + stats.failedArticles() + "件");
        }
        return new WeeklyRunResult(report.path(), report.markdown(), report.articles(), report.keywords(),
                report.summaries(), logs, stats);
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
                new ArticleSummaryRepository(database).findAll(), List.of(), null);
    }
}
