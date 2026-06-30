package com.example.techwatch.db;

import com.example.techwatch.keyword.KeywordWeeklyStats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class KeywordWeeklyStatsRepository {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Tokyo");
    private final Database database;

    public KeywordWeeklyStatsRepository(Database database) { this.database = database; }

    public void capture(LocalDate weekStart) throws SQLException {
        Instant start = weekStart.atStartOfDay(APP_ZONE).toInstant();
        Instant end = weekStart.plusDays(7).atStartOfDay(APP_ZONE).toInstant();
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
                while (result.next()) captured.add(new KeywordWeeklyStats(result.getLong("keyword_id"), weekStart,
                        result.getInt("mention_count"), result.getInt("source_count"),
                        result.getInt("official_source_count"), result.getInt("high_score_article_count"),
                        result.getInt("report_included_count"), result.getDouble("average_article_score")));
            }
        }
        for (KeywordWeeklyStats value : captured) save(value);
    }

    public void save(KeywordWeeklyStats stats) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keyword_weekly_stats(keyword_id,week_start,mention_count,source_count,
                  official_source_count,high_score_article_count,report_included_count,average_article_score,
                  created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(keyword_id,week_start) DO UPDATE SET mention_count=excluded.mention_count,
                  source_count=excluded.source_count,official_source_count=excluded.official_source_count,
                  high_score_article_count=excluded.high_score_article_count,
                  report_included_count=excluded.report_included_count,
                  average_article_score=excluded.average_article_score,updated_at=excluded.updated_at
                """)) {
            statement.setLong(1, stats.keywordId()); statement.setString(2, stats.weekStart().toString());
            statement.setInt(3, stats.mentionCount()); statement.setInt(4, stats.sourceCount());
            statement.setInt(5, stats.officialSourceCount()); statement.setInt(6, stats.highScoreArticleCount());
            statement.setInt(7, stats.reportIncludedCount()); statement.setDouble(8, stats.averageArticleScore());
            statement.setString(9, now); statement.setString(10, now); statement.executeUpdate();
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

    private KeywordWeeklyStats map(ResultSet result) throws SQLException {
        return new KeywordWeeklyStats(result.getLong("keyword_id"), LocalDate.parse(result.getString("week_start")),
                result.getInt("mention_count"), result.getInt("source_count"),
                result.getInt("official_source_count"), result.getInt("high_score_article_count"),
                result.getInt("report_included_count"), result.getDouble("average_article_score"));
    }
}
