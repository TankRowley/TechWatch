package com.example.techwatch.db;

import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordEvaluationResult;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class KeywordRepository {
    private final Database database;

    public KeywordRepository(Database database) { this.database = database; }

    public Keyword save(Keyword keyword) throws SQLException {
        String now = Instant.now().toString();
        String sql = """
                INSERT INTO keywords(name,normalized_name,category,status,weight,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?)
                ON CONFLICT(normalized_name) DO UPDATE SET name=excluded.name,category=excluded.category,
                  weight=excluded.weight,updated_at=excluded.updated_at,
                  status=CASE WHEN keywords.first_seen_at IS NULL THEN excluded.status ELSE keywords.status END
                """;
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, keyword.getName());
            statement.setString(2, keyword.getNormalizedName());
            statement.setString(3, keyword.getCategory());
            statement.setString(4, keyword.getStatus());
            statement.setInt(5, keyword.getWeight());
            statement.setString(6, now);
            statement.setString(7, now);
            statement.executeUpdate();
        }
        return findByNormalizedName(keyword.getNormalizedName());
    }

    public Keyword findByNormalizedName(String normalizedName) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM keywords WHERE normalized_name=?")) {
            statement.setString(1, normalizedName);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("保存したキーワードを取得できません: " + normalizedName);
                return map(result);
            }
        }
    }

    public List<Keyword> findAll() throws SQLException {
        List<Keyword> keywords = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM keywords ORDER BY final_score DESC, name"); ResultSet result = statement.executeQuery()) {
            while (result.next()) keywords.add(map(result));
        }
        return keywords;
    }

    public void updateSeenAt(long keywordId, Instant seenAt) throws SQLException {
        String value = seenAt.toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE keywords SET
                  first_seen_at=CASE WHEN first_seen_at IS NULL OR first_seen_at>? THEN ? ELSE first_seen_at END,
                  last_seen_at=CASE WHEN last_seen_at IS NULL OR last_seen_at<? THEN ? ELSE last_seen_at END,
                  updated_at=? WHERE id=?
                """)) {
            statement.setString(1, value); statement.setString(2, value);
            statement.setString(3, value); statement.setString(4, value);
            statement.setString(5, Instant.now().toString()); statement.setLong(6, keywordId);
            statement.executeUpdate();
        }
    }

    public void updateEvaluation(long keywordId, KeywordEvaluationResult evaluation) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE keywords SET trend_score=?,stability_score=?,learning_value_score=?,buzz_risk_score=?,
                  final_score=?,status=?,updated_at=? WHERE id=?
                """)) {
            statement.setDouble(1, evaluation.trendScore());
            statement.setDouble(2, evaluation.stabilityScore());
            statement.setDouble(3, evaluation.learningValueScore());
            statement.setDouble(4, evaluation.buzzRiskScore());
            statement.setDouble(5, evaluation.finalScore());
            statement.setString(6, evaluation.status());
            statement.setString(7, Instant.now().toString());
            statement.setLong(8, keywordId);
            statement.executeUpdate();
        }
    }

    public void updatePinned(long keywordId, boolean pinned, String reason) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE keywords SET pinned=?,pinned_at=?,pin_reason=?,updated_at=? WHERE id=?
                """)) {
            statement.setInt(1, pinned ? 1 : 0);
            statement.setString(2, pinned ? now : null);
            statement.setString(3, pinned ? (reason == null ? "" : reason.trim()) : "");
            statement.setString(4, now);
            statement.setLong(5, keywordId);
            statement.executeUpdate();
        }
    }

    public void updateLearning(long keywordId, boolean learning, String reason) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE keywords SET learning=?,learning_since=?,learning_reason=?,updated_at=? WHERE id=?
                """)) {
            statement.setInt(1, learning ? 1 : 0);
            statement.setString(2, learning ? now : null);
            statement.setString(3, learning ? (reason == null ? "" : reason.trim()) : "");
            statement.setString(4, now);
            statement.setLong(5, keywordId);
            statement.executeUpdate();
        }
    }

    private Keyword map(ResultSet result) throws SQLException {
        return new Keyword(result.getLong("id"), result.getString("name"), result.getString("normalized_name"),
                result.getString("category"), result.getString("status"), result.getInt("weight"),
                result.getDouble("trend_score"), result.getDouble("stability_score"), result.getDouble("market_score"),
                result.getDouble("learning_value_score"), result.getDouble("buzz_risk_score"),
                result.getDouble("final_score"), DbTime.instant(result.getString("first_seen_at")),
                DbTime.instant(result.getString("last_seen_at")), result.getInt("pinned") == 1,
                DbTime.instant(result.getString("pinned_at")), result.getString("pin_reason"),
                result.getInt("learning") == 1, DbTime.instant(result.getString("learning_since")),
                result.getString("learning_reason"));
    }
}
