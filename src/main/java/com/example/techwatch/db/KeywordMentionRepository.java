package com.example.techwatch.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class KeywordMentionRepository {
    private final Database database;

    public KeywordMentionRepository(Database database) { this.database = database; }

    public boolean saveMention(long articleId, long keywordId, String detectedIn, Instant observedAt) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO keyword_mentions(article_id,keyword_id,detected_in,observed_at,created_at)
                VALUES(?,?,?,?,?)
                """)) {
            statement.setLong(1, articleId);
            statement.setLong(2, keywordId);
            statement.setString(3, detectedIn);
            statement.setString(4, observedAt.toString());
            statement.setString(5, now);
            return statement.executeUpdate() > 0;
        }
    }

    public List<KeywordStats> findStats(Instant weekStart, Instant weekEnd) throws SQLException {
        Instant previousStart = weekStart.minusSeconds(7L * 24 * 60 * 60);
        Instant fourWeekStart = weekStart.minusSeconds(21L * 24 * 60 * 60);
        String sql = """
                SELECT k.id AS keyword_id,
                  COUNT(DISTINCT CASE WHEN m.observed_at>=? AND m.observed_at<? THEN m.article_id END) AS current_count,
                  COUNT(DISTINCT CASE WHEN m.observed_at>=? AND m.observed_at<? THEN m.article_id END) AS previous_count,
                  COUNT(DISTINCT CASE WHEN m.observed_at>=? AND m.observed_at<? THEN strftime('%Y-%W',m.observed_at) END) AS active_weeks,
                  COUNT(DISTINCT CASE WHEN m.observed_at>=? AND m.observed_at<? THEN a.source_id END) AS source_diversity,
                  COALESCE(AVG(CASE WHEN m.observed_at>=? AND m.observed_at<? THEN a.article_score END),0) AS average_score
                FROM keywords k
                LEFT JOIN keyword_mentions m ON m.keyword_id=k.id
                LEFT JOIN articles a ON a.id=m.article_id
                GROUP BY k.id
                """;
        List<KeywordStats> stats = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, weekStart.toString()); statement.setString(i++, weekEnd.toString());
            statement.setString(i++, previousStart.toString()); statement.setString(i++, weekStart.toString());
            statement.setString(i++, fourWeekStart.toString()); statement.setString(i++, weekEnd.toString());
            statement.setString(i++, weekStart.toString()); statement.setString(i++, weekEnd.toString());
            statement.setString(i++, weekStart.toString()); statement.setString(i, weekEnd.toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    stats.add(new KeywordStats(result.getLong("keyword_id"), result.getInt("current_count"),
                            result.getInt("previous_count"), result.getInt("active_weeks"),
                            result.getInt("source_diversity"), result.getDouble("average_score")));
                }
            }
        }
        return stats;
    }
}
