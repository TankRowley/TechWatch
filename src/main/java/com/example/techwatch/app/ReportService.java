package com.example.techwatch.app;

import com.example.techwatch.article.Article;
import com.example.techwatch.db.ArticleRepository;
import com.example.techwatch.db.ArticleSummaryRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.ReportRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.report.MarkdownReportWriter;
import com.example.techwatch.report.WeeklyReport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ArticleRepository articleRepository;
    private final ArticleSummaryRepository summaryRepository;
    private final KeywordRepository keywordRepository;
    private final ReportRepository reportRepository;
    private final MarkdownReportWriter writer;

    public ReportService(ArticleRepository articleRepository, ArticleSummaryRepository summaryRepository,
                         KeywordRepository keywordRepository, ReportRepository reportRepository,
                         MarkdownReportWriter writer) {
        this.articleRepository = articleRepository;
        this.summaryRepository = summaryRepository;
        this.keywordRepository = keywordRepository;
        this.reportRepository = reportRepository;
        this.writer = writer;
    }

    public ReportOutput generate(LocalDate periodStart, LocalDate periodEnd, Instant start, Instant end, Path reportsDirectory)
            throws Exception {
        List<Article> articles = articleRepository.findBetween(start, end);
        List<Keyword> keywords = keywordRepository.findAll();
        WeeklyReport report = new WeeklyReport(periodStart, periodEnd, articles, keywords, summaryRepository.findAll());
        Path path = writer.write(report, reportsDirectory);
        reportRepository.save(periodEnd, path, articles);
        return new ReportOutput(path, Files.readString(path), articles, keywords);
    }

    public record ReportOutput(Path path, String markdown, List<Article> articles, List<Keyword> keywords) { }
}
