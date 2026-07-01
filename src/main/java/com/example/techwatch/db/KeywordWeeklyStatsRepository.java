package com.example.techwatch.db;

import com.example.techwatch.keyword.KeywordWeeklyStats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeywordWeeklyStatsRepository {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Tokyo");
    private final Database database;

    public KeywordWeeklyStatsRepository(Database database) { this.database = database; }

    public void capture(LocalDate weekStart) throws SQLException {
        capture(weekStart, 0, 0);
    }

    public void capture(LocalDate weekStart, int successfulSources, int configuredSources) throws SQLException {
        capture(weekStart, successfulSources, configuredSources, null);
    }

    private void capture(LocalDate weekStart, int successfulSources, int configuredSources,
                         String forcedStatus) throws SQLException {
        Instant start = weekStart.atStartOfDay(APP_ZONE).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(APP_ZONE).toInstant();
        Map<Long, Double> concentration = sourceConcentration(start, end);
        Map<Long, Double> categoryConcentration = categoryConcentration(start, end);
        int totalArticles = totalArticles(start, end);
        String collectionStatus = forcedStatus != null ? forcedStatus : configuredSources <= 0 ? "LEGACY"
                : successfulSources <= 0 ? "MISSING"
                : successfulSources < configuredSources ? "PARTIAL" : "SUCCESS";
        String sql = """
                WITH matched AS (
                  SELECT DISTINCT m.keyword_id,m.article_id
                  FROM keyword_mentions m WHERE m.observed_at>=? AND m.observed_at<?
                )
                SELECT k.id AS keyword_id,COUNT(ma.article_id) AS mention_count,
                  COUNT(DISTINCT a.source_id) AS source_count,
                  COUNT(DISTINCT CASE WHEN s.trust_score>=4 THEN a.id END) AS official_source_count,
                  COUNT(DISTINCT CASE WHEN a.article_score>=8 THEN a.id END) AS high_score_article_count,
                  COUNT(DISTINCT CASE WHEN wr.id IS NOT NULL THEN ri.article_id END) AS report_included_count,
                  COALESCE(AVG(a.article_score),0) AS average_article_score
                FROM keywords k
                LEFT JOIN matched ma ON ma.keyword_id=k.id
                LEFT JOIN articles a ON a.id=ma.article_id
                LEFT JOIN sources s ON s.id=a.source_id
                LEFT JOIN report_items ri ON ri.article_id=a.id
                LEFT JOIN weekly_reports wr ON wr.id=ri.report_id AND wr.report_date>=? AND wr.report_date<?
                GROUP BY k.id
                """;
        List<KeywordWeeklyStats> captured = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, start.toString()); statement.setString(2, end.toString());
            statement.setString(3, weekStart.toString()); statement.setString(4, weekStart.plusDays(7).toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long keywordId = result.getLong("keyword_id");
                    captured.add(new KeywordWeeklyStats(keywordId, weekStart,
                        result.getInt("mention_count"), result.getInt("source_count"),
                        result.getInt("official_source_count"), result.getInt("high_score_article_count"),
                        result.getInt("report_included_count"), result.getDouble("average_article_score"),
                        totalArticles, successfulSources, configuredSources, collectionStatus,
                        concentration.getOrDefault(keywordId, 1.0),
                        categoryConcentration.getOrDefault(keywordId, 1.0)));
                }
            }
        }
        for (KeywordWeeklyStats value : captured) save(value);
    }

    public int backfillHistoricalWeeks(LocalDate latestCompletedWeek, int maximumWeeks,
                                       int configuredSources) throws SQLException {
        int captured = 0;
        int weeks = Math.max(1, Math.min(52, maximumWeeks));
        for (int age = weeks - 1; age >= 1; age--) {
            LocalDate week = latestCompletedWeek.minusWeeks(age);
            Instant start = week.atStartOfDay(APP_ZONE).toInstant();
            Instant end = week.plusDays(7).atStartOfDay(APP_ZONE).toInstant();
            if (totalArticles(start, end) <= 0) continue;
            capture(week, distinctArticleSources(start, end), configuredSources, "HISTORICAL_PARTIAL");
            captured++;
        }
        return captured;
    }

    public boolean hasAnyStats() throws SQLException {
        try (var connection = database.connect(); var statement = connection.prepareStatement(
                "SELECT EXISTS(SELECT 1 FROM keyword_weekly_stats LIMIT 1)");
             var result = statement.executeQuery()) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private int totalArticles(Instant start, Instant end) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM articles WHERE COALESCE(published_at,fetched_at)>=? AND COALESCE(published_at,fetched_at)<?")) {
            statement.setString(1, start.toString());
            statement.setString(2, end.toString());
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : 0; }
        }
    }

    private int distinctArticleSources(Instant start, Instant end) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(DISTINCT source_id) FROM articles WHERE COALESCE(published_at,fetched_at)>=? AND COALESCE(published_at,fetched_at)<?")) {
            statement.setString(1, start.toString()); statement.setString(2, end.toString());
            try (ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : 0; }
        }
    }

    private Map<Long, Double> sourceConcentration(Instant start, Instant end) throws SQLException {
        Map<Long, Double> values = new HashMap<>();
        String sql = """
                SELECT keyword_id,MAX(source_mentions)*1.0/SUM(source_mentions) concentration
                FROM (
                  SELECT m.keyword_id,a.source_id,COUNT(DISTINCT m.article_id) source_mentions
                  FROM keyword_mentions m JOIN articles a ON a.id=m.article_id
                  WHERE m.observed_at>=? AND m.observed_at<?
                  GROUP BY m.keyword_id,a.source_id
                ) GROUP BY keyword_id
                """;
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, start.toString()); statement.setString(2, end.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.put(result.getLong("keyword_id"), result.getDouble("concentration"));
            }
        }
        return values;
    }

    private Map<Long, Double> categoryConcentration(Instant start, Instant end) throws SQLException {
        Map<Long, Double> values = new HashMap<>();
        String sql = """
                SELECT keyword_id,MAX(category_mentions)*1.0/SUM(category_mentions) concentration
                FROM (
                  SELECT m.keyword_id,s.source_category,COUNT(DISTINCT m.article_id) category_mentions
                  FROM keyword_mentions m JOIN articles a ON a.id=m.article_id
                  JOIN sources s ON s.id=a.source_id
                  WHERE m.observed_at>=? AND m.observed_at<?
                  GROUP BY m.keyword_id,s.source_category
                ) GROUP BY keyword_id
                """;
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, start.toString()); statement.setString(2, end.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.put(result.getLong("keyword_id"), result.getDouble("concentration"));
            }
        }
        return values;
    }

    public void save(KeywordWeeklyStats stats) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keyword_weekly_stats(keyword_id,week_start,mention_count,source_count,
                  official_source_count,high_score_article_count,report_included_count,average_article_score,
                  total_article_count,successful_source_count,configured_source_count,collection_status,
                  source_concentration,category_concentration,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(keyword_id,week_start) DO UPDATE SET mention_count=excluded.mention_count,
                  source_count=excluded.source_count,official_source_count=excluded.official_source_count,
                  high_score_article_count=excluded.high_score_article_count,
                  report_included_count=excluded.report_included_count,
                  average_article_score=excluded.average_article_score,
                  total_article_count=excluded.total_article_count,
                  successful_source_count=excluded.successful_source_count,
                  configured_source_count=excluded.configured_source_count,
                  collection_status=excluded.collection_status,
                  source_concentration=excluded.source_concentration,
                  category_concentration=excluded.category_concentration,updated_at=excluded.updated_at
                """)) {
            statement.setLong(1, stats.keywordId()); statement.setString(2, stats.weekStart().toString());
            statement.setInt(3, stats.mentionCount()); statement.setInt(4, stats.sourceCount());
            statement.setInt(5, stats.officialSourceCount()); statement.setInt(6, stats.highScoreArticleCount());
            statement.setInt(7, stats.reportIncludedCount()); statement.setDouble(8, stats.averageArticleScore());
            statement.setInt(9, stats.totalArticleCount()); statement.setInt(10, stats.successfulSourceCount());
            statement.setInt(11, stats.configuredSourceCount()); statement.setString(12, stats.collectionStatus());
            statement.setDouble(13, stats.sourceConcentration());
            statement.setDouble(14, stats.categoryConcentration());
            statement.setString(15, now); statement.setString(16, now); statement.executeUpdate();
        }
    }

    public void refreshReportIncludedCounts(LocalDate weekStart) throws SQLException {
        Instant start = weekStart.atStartOfDay(APP_ZONE).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(APP_ZONE).toInstant();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE keyword_weekly_stats
                SET report_included_count=(
                  SELECT COUNT(DISTINCT km.article_id)
                  FROM keyword_mentions km
                  JOIN report_items ri ON ri.article_id=km.article_id
                  JOIN weekly_reports wr ON wr.id=ri.report_id
                  WHERE km.keyword_id=keyword_weekly_stats.keyword_id
                    AND km.observed_at>=? AND km.observed_at<?
                    AND wr.report_date>=? AND wr.report_date<?
                ),updated_at=?
                WHERE week_start=?
                """)) {
            statement.setString(1, start.toString());
            statement.setString(2, end.toString());
            statement.setString(3, weekStart.toString());
            statement.setString(4, weekStart.plusDays(7).toString());
            statement.setString(5, Instant.now().toString());
            statement.setString(6, weekStart.toString());
            statement.executeUpdate();
        }
    }

    public List<KeywordWeeklyStats> findRecent(long keywordId, int weeks) throws SQLException {
        List<KeywordWeeklyStats> values = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM (SELECT * FROM keyword_weekly_stats WHERE keyword_id=?
                  ORDER BY week_start DESC LIMIT ?) ORDER BY week_start
                """)) {
            statement.setLong(1, keywordId); statement.setInt(2, weeks);
            try (ResultSet result = statement.executeQuery()) { while (result.next()) values.add(map(result)); }
        }
        return values;
    }

    public List<KeywordWeeklyStats> findSince(long keywordId, LocalDate firstWeek) throws SQLException {
        List<KeywordWeeklyStats> values = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM keyword_weekly_stats WHERE keyword_id=? AND week_start>=? ORDER BY week_start")) {
            statement.setLong(1, keywordId); statement.setString(2, firstWeek.toString());
            try (ResultSet result = statement.executeQuery()) { while (result.next()) values.add(map(result)); }
        }
        return values;
    }

    private KeywordWeeklyStats map(ResultSet result) throws SQLException {
        return new KeywordWeeklyStats(result.getLong("keyword_id"), LocalDate.parse(result.getString("week_start")),
                result.getInt("mention_count"), result.getInt("source_count"),
                result.getInt("official_source_count"), result.getInt("high_score_article_count"),
                result.getInt("report_included_count"), result.getDouble("average_article_score"),
                result.getInt("total_article_count"), result.getInt("successful_source_count"),
                result.getInt("configured_source_count"), result.getString("collection_status"),
                result.getDouble("source_concentration"), result.getDouble("category_concentration"));
    }
}
