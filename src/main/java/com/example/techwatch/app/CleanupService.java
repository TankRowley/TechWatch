package com.example.techwatch.app;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.RetentionPolicy;
import com.example.techwatch.db.Database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class CleanupService {
    private static final double HIGH_SCORE_PROTECTION = 8.0;
    private static final String UNPROTECTED_ARTICLE = """
            saved_by_user=0 AND cleanup_protected=0 AND article_score<?
            AND NOT EXISTS (SELECT 1 FROM report_items ri WHERE ri.article_id=articles.id)
            AND NOT EXISTS (
              SELECT 1 FROM keyword_mentions km JOIN keywords k ON k.id=km.keyword_id
              WHERE km.article_id=articles.id AND (k.pinned=1 OR k.learning=1)
            )
            """;
    private static final String WEEKLY_STATS_READY = """
            AND NOT EXISTS (
              SELECT 1 FROM keyword_mentions km
              WHERE km.article_id=articles.id AND NOT EXISTS (
                SELECT 1 FROM keyword_weekly_stats kws
                WHERE kws.keyword_id=km.keyword_id
                  AND date(km.observed_at)>=date(kws.week_start)
                  AND date(km.observed_at)<date(kws.week_start,'+7 days')
              )
            )
            """;

    private final Database database;
    private final AppPaths paths;
    private final RetentionPolicy policy;
    private final Clock clock;

    public CleanupService(Database database, AppPaths paths, RetentionPolicy policy) {
        this(database, paths, policy, Clock.systemUTC());
    }

    CleanupService(Database database, AppPaths paths, RetentionPolicy policy, Clock clock) {
        this.database = database;
        this.paths = paths;
        this.policy = policy;
        this.clock = clock;
    }

    public CleanupResult cleanup() throws SQLException, IOException { return cleanup(false); }

    public CleanupResult cleanup(boolean vacuum) throws SQLException, IOException {
        paths.ensureDirectories();
        database.initialize();
        long before = databaseSize();
        Instant now = clock.instant();
        DbCounts counts = cleanupDatabase(now);
        int logs = deleteOldFiles(paths.logsDirectory(), now.minus(policy.executionLogDays(), ChronoUnit.DAYS), null);
        int html = deleteOldFiles(paths.reportsDirectory(), now.minus(policy.htmlReportDays(), ChronoUnit.DAYS), ".html");
        if (vacuum) vacuum();
        return new CleanupResult(counts.bodyTexts(), counts.rawHtml(), counts.summaries(), counts.articles(),
                counts.jobSnapshots(), logs, html, before, databaseSize(), vacuum);
    }

    public long databaseSize() throws IOException {
        return Files.exists(paths.database()) ? Files.size(paths.database()) : 0;
    }

    public void vacuum() throws SQLException {
        try (Connection connection = database.connect(); var statement = connection.createStatement()) {
            statement.execute("VACUUM");
        }
    }

    private DbCounts cleanupDatabase(Instant now) throws SQLException {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                int rawHtml = clearBodyColumn(connection, "raw_html", cutoff(now, policy.rawHtmlDays()));
                int bodyTexts = clearBodyColumn(connection, "body_text", cutoff(now, policy.articleBodyDays()));
                markArchivedBodies(connection);
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("DELETE FROM article_bodies WHERE body_text IS NULL AND raw_html IS NULL");
                }
                int summaries = deleteSummaries(connection, cutoff(now, policy.articleMetadataDays()));
                int lowPriority = deleteArticles(connection, cutoff(now, policy.unselectedArticleDays()), true);
                int oldMetadata = deleteArticles(connection, cutoff(now, policy.articleMetadataDays()), false);
                int snapshots = executeCutoff(connection,
                        "DELETE FROM job_market_snapshots WHERE fetched_at<?", cutoff(now, policy.jobSnapshotDays()));
                connection.commit();
                return new DbCounts(bodyTexts, rawHtml, summaries, lowPriority + oldMetadata, snapshots);
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException(error);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int clearBodyColumn(Connection connection, String column, String cutoff) throws SQLException {
        String sql = "UPDATE article_bodies SET " + column + "=NULL,updated_at=? WHERE " + column
                + " IS NOT NULL AND fetched_at<? AND article_id IN (SELECT articles.id FROM articles WHERE "
                + UNPROTECTED_ARTICLE + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clock.instant().toString());
            statement.setString(2, cutoff);
            statement.setDouble(3, HIGH_SCORE_PROTECTION);
            return statement.executeUpdate();
        }
    }

    private void markArchivedBodies(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE articles SET archived=1
                    WHERE id IN (SELECT article_id FROM article_bodies WHERE body_text IS NULL AND raw_html IS NULL)
                    """);
        }
    }

    private int deleteSummaries(Connection connection, String cutoff) throws SQLException {
        String sql = "DELETE FROM article_summaries WHERE updated_at<? AND article_id IN "
                + "(SELECT articles.id FROM articles WHERE " + UNPROTECTED_ARTICLE + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cutoff);
            statement.setDouble(2, HIGH_SCORE_PROTECTION);
            return statement.executeUpdate();
        }
    }

    private int deleteArticles(Connection connection, String cutoff, boolean lowPriorityOnly) throws SQLException {
        String label = lowPriorityOnly ? " AND importance_label IN ('Ignore','Archive','UNRATED')" : "";
        String sql = "DELETE FROM articles WHERE COALESCE(published_at,fetched_at)<? AND "
                + UNPROTECTED_ARTICLE + WEEKLY_STATS_READY + label;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cutoff);
            statement.setDouble(2, HIGH_SCORE_PROTECTION);
            return statement.executeUpdate();
        }
    }

    private int executeCutoff(Connection connection, String sql, String cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, cutoff);
            return statement.executeUpdate();
        }
    }

    private int deleteOldFiles(Path directory, Instant cutoff, String extension) throws IOException {
        if (!Files.exists(directory)) return 0;
        int deleted = 0;
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                if (extension != null && !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension)) continue;
                if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff) && Files.deleteIfExists(file)) deleted++;
            }
        }
        return deleted;
    }

    private String cutoff(Instant now, int days) { return now.minus(days, ChronoUnit.DAYS).toString(); }

    private record DbCounts(int bodyTexts, int rawHtml, int summaries, int articles, int jobSnapshots) { }
}
