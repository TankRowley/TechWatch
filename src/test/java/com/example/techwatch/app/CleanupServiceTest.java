package com.example.techwatch.app;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.RetentionPolicy;
import com.example.techwatch.db.Database;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanupServiceTest {
    private static final Instant NOW = Instant.parse("2026-06-30T00:00:00Z");
    @TempDir Path temp;

    @Test
    void removesExpiredDetailsAndPreservesTrendDataAndProtectedArticles() throws Exception {
        AppPaths paths = new AppPaths(temp);
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();

        try (Connection connection = database.connect()) {
            long deletable = article(connection, "delete", 1, "Ignore", 200, false);
            long highScore = article(connection, "high", 10, "Watch", 500, false);
            long saved = article(connection, "saved", 1, "Ignore", 500, true);
            long reported = article(connection, "reported", 1, "Ignore", 500, false);
            long pinned = article(connection, "pinned", 1, "Ignore", 500, false);
            long missingStats = article(connection, "missing-stats", 1, "Ignore", 500, false);
            long aggregated = article(connection, "aggregated", 1, "Ignore", 500, false);
            long bodyOnly = article(connection, "body", 1, "Skim", 70, false);
            long summaryOnly = article(connection, "summary", 5, "Skim", 350, false);

            long pinnedKeyword = keyword(connection, "Pinned", true);
            long ordinaryKeyword = keyword(connection, "Ordinary", false);
            long missingStatsKeyword = keyword(connection, "No Stats", false);
            mention(connection, pinned, pinnedKeyword, 500);
            mention(connection, missingStats, missingStatsKeyword, 500);
            mention(connection, aggregated, ordinaryKeyword, 500);
            weeklyStats(connection, ordinaryKeyword, LocalDate.of(2025, 2, 10));
            marketStats(connection, ordinaryKeyword);
            report(connection, reported);

            body(connection, bodyOnly, 70);
            summary(connection, summaryOnly, 400);
            jobSnapshot(connection, ordinaryKeyword, 400);

            assertTrue(exists(connection, deletable));
            assertTrue(exists(connection, highScore));
            assertTrue(exists(connection, saved));
        }

        Path oldLog = Files.writeString(paths.logsDirectory().resolve("old.log"), "old");
        Path recentLog = Files.writeString(paths.logsDirectory().resolve("recent.log"), "recent");
        Path oldHtml = Files.writeString(paths.reportsDirectory().resolve("old.html"), "old");
        Path oldMarkdown = Files.writeString(paths.reportsDirectory().resolve("old.md"), "keep");
        FileTime oldTime = FileTime.from(NOW.minusSeconds(400L * 86_400));
        Files.setLastModifiedTime(oldLog, oldTime);
        Files.setLastModifiedTime(oldHtml, oldTime);
        Files.setLastModifiedTime(oldMarkdown, oldTime);

        CleanupService service = new CleanupService(database, paths, RetentionPolicy.defaults(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        CleanupResult result = service.cleanup();

        assertEquals(0, result.bodyTextsCleared());
        assertEquals(1, result.rawHtmlCleared());
        assertEquals(0, result.summariesDeleted());
        assertEquals(1, result.articlesDeleted());
        assertEquals(0, result.jobSnapshotsDeleted());
        assertEquals(1, result.logFilesDeleted());
        assertEquals(0, result.htmlReportsDeleted());
        assertFalse(Files.exists(oldLog));
        assertTrue(Files.exists(recentLog));
        assertTrue(Files.exists(oldHtml));
        assertTrue(Files.exists(oldMarkdown));

        try (Connection connection = database.connect()) {
            assertTrue(existsByTitle(connection, "delete"));
            assertFalse(existsByTitle(connection, "aggregated"));
            assertTrue(existsByTitle(connection, "high"));
            assertTrue(existsByTitle(connection, "saved"));
            assertTrue(existsByTitle(connection, "reported"));
            assertTrue(existsByTitle(connection, "pinned"));
            assertTrue(existsByTitle(connection, "missing-stats"));
            assertEquals(1, count(connection, "keyword_weekly_stats"));
            assertEquals(1, count(connection, "keyword_market_stats"));
            assertEquals(1, count(connection, "article_bodies"));
            assertEquals(0, scalar(connection, "SELECT archived FROM articles WHERE title='body'"));
        }
    }

    @Test
    void removesOptionalHistoryWhenRetentionFlagsAreDisabled() throws Exception {
        AppPaths paths = new AppPaths(temp);
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();
        try (Connection connection = database.connect()) {
            long keyword = keyword(connection, "Optional History", false);
            weeklyStats(connection, keyword, LocalDate.of(2025, 1, 6));
            marketStats(connection, keyword);
        }
        Path markdown = Files.writeString(paths.reportsDirectory().resolve("old.md"), "old");
        Files.setLastModifiedTime(markdown, FileTime.from(NOW.minusSeconds(400L * 86_400)));
        RetentionPolicy policy = new RetentionPolicy(60, 30, 30, 180, 365, 365, 365,
                false, false, false);

        CleanupResult result = new CleanupService(database, paths, policy,
                Clock.fixed(NOW, ZoneOffset.UTC)).cleanup();

        assertEquals(1, result.markdownReportsDeleted());
        assertEquals(1, result.weeklyKeywordStatsDeleted());
        assertEquals(1, result.keywordMarketStatsDeleted());
        assertFalse(Files.exists(markdown));
        try (Connection connection = database.connect()) {
            assertEquals(0, count(connection, "keyword_weekly_stats"));
            assertEquals(0, count(connection, "keyword_market_stats"));
        }
    }

    private long article(Connection connection, String title, double score, String label, int ageDays, boolean saved)
            throws Exception {
        String time = NOW.minusSeconds(ageDays * 86_400L).toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO articles(title,url,published_at,fetched_at,summary_original,body_status,article_score,
                  importance_label,saved_by_user,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title); statement.setString(2, "https://example.com/" + title);
            statement.setString(3, time); statement.setString(4, time); statement.setString(5, "summary");
            statement.setString(6, "SUCCESS"); statement.setDouble(7, score); statement.setString(8, label);
            statement.setInt(9, saved ? 1 : 0); statement.setString(10, time); statement.setString(11, time);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private long keyword(Connection connection, String name, boolean pinned) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keywords(name,normalized_name,pinned,created_at,updated_at) VALUES(?,?,?,?,?)
                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, name); statement.setString(2, name.toLowerCase());
            statement.setInt(3, pinned ? 1 : 0); statement.setString(4, NOW.toString()); statement.setString(5, NOW.toString());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private void mention(Connection connection, long articleId, long keywordId, int ageDays) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keyword_mentions(article_id,keyword_id,detected_in,observed_at,created_at)
                VALUES(?,?,'title',?,?)
                """)) {
            String time = NOW.minusSeconds(ageDays * 86_400L).toString();
            statement.setLong(1, articleId); statement.setLong(2, keywordId);
            statement.setString(3, time); statement.setString(4, time); statement.executeUpdate();
        }
    }

    private void weeklyStats(Connection connection, long keywordId, LocalDate weekStart) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keyword_weekly_stats(keyword_id,week_start,created_at,updated_at) VALUES(?,?,?,?)
                """)) {
            statement.setLong(1, keywordId); statement.setString(2, weekStart.toString());
            statement.setString(3, NOW.toString()); statement.setString(4, NOW.toString()); statement.executeUpdate();
        }
    }

    private void marketStats(Connection connection, long keywordId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keyword_market_stats(keyword_id,week_start,created_at,updated_at) VALUES(?,?,?,?)
                """)) {
            statement.setLong(1, keywordId); statement.setString(2, "2025-01-06");
            statement.setString(3, NOW.toString()); statement.setString(4, NOW.toString()); statement.executeUpdate();
        }
    }

    private void report(Connection connection, long articleId) throws Exception {
        connection.createStatement().executeUpdate("""
                INSERT INTO weekly_reports(report_date,file_path,article_count,created_at)
                VALUES('2025-01-01','report.md',1,'2025-01-01T00:00:00Z')
                """);
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO report_items(report_id,article_id,rank,created_at) VALUES(1,?,1,?)
                """)) {
            statement.setLong(1, articleId); statement.setString(2, NOW.toString()); statement.executeUpdate();
        }
    }

    private void body(Connection connection, long articleId, int ageDays) throws Exception {
        String time = NOW.minusSeconds(ageDays * 86_400L).toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO article_bodies(article_id,body_text,raw_html,fetched_at,created_at,updated_at)
                VALUES(?,'body','<html>raw</html>',?,?,?)
                """)) {
            statement.setLong(1, articleId); statement.setString(2, time);
            statement.setString(3, time); statement.setString(4, time); statement.executeUpdate();
        }
    }

    private void summary(Connection connection, long articleId, int ageDays) throws Exception {
        String time = NOW.minusSeconds(ageDays * 86_400L).toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO article_summaries(article_id,short_summary,created_at,updated_at) VALUES(?,'AI',?,?)
                """)) {
            statement.setLong(1, articleId); statement.setString(2, time); statement.setString(3, time);
            statement.executeUpdate();
        }
    }

    private void jobSnapshot(Connection connection, long keywordId, int ageDays) throws Exception {
        String time = NOW.minusSeconds(ageDays * 86_400L).toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO job_market_snapshots(keyword_id,region,source_name,query,job_count,fetched_at,week_start,created_at)
                VALUES(?,'US','manual','query',1,?,'2025-05-26',?)
                """)) {
            statement.setLong(1, keywordId); statement.setString(2, time); statement.setString(3, time);
            statement.executeUpdate();
        }
    }

    private boolean exists(Connection connection, long id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM articles WHERE id=?")) {
            statement.setLong(1, id); try (var result = statement.executeQuery()) { return result.next(); }
        }
    }

    private boolean existsByTitle(Connection connection, String title) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM articles WHERE title=?")) {
            statement.setString(1, title); try (var result = statement.executeQuery()) { return result.next(); }
        }
    }

    private int count(Connection connection, String table) throws Exception {
        return scalar(connection, "SELECT COUNT(*) FROM " + table);
    }

    private int scalar(Connection connection, String sql) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
