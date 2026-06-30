package com.example.techwatch.db;

import com.example.techwatch.summarize.ArticleSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArticleSummaryRepository {
    private final Database database;
    private final ObjectMapper mapper = new ObjectMapper();

    public ArticleSummaryRepository(Database database) { this.database = database; }

    public void save(long articleId, ArticleSummary summary) throws SQLException {
        String now = Instant.now().toString();
        String sql = """
                INSERT INTO article_summaries(article_id,short_summary,technical_points,why_it_matters,
                  learning_priority,prerequisites,related_keywords,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?,?,?)
                ON CONFLICT(article_id) DO UPDATE SET short_summary=excluded.short_summary,
                  technical_points=excluded.technical_points,why_it_matters=excluded.why_it_matters,
                  learning_priority=excluded.learning_priority,prerequisites=excluded.prerequisites,
                  related_keywords=excluded.related_keywords,updated_at=excluded.updated_at
                """;
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, articleId);
            statement.setString(2, summary.shortSummary());
            statement.setString(3, json(summary.technicalPoints()));
            statement.setString(4, summary.whyItMatters());
            statement.setString(5, summary.learningPriority());
            statement.setString(6, json(summary.prerequisites()));
            statement.setString(7, json(summary.relatedKeywords()));
            statement.setString(8, now); statement.setString(9, now);
            statement.executeUpdate();
        }
    }

    public Map<Long, ArticleSummary> findAll() throws SQLException {
        Map<Long, ArticleSummary> summaries = new LinkedHashMap<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT s.*,a.importance_label FROM article_summaries s JOIN articles a ON a.id=s.article_id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                summaries.put(result.getLong("article_id"), new ArticleSummary(result.getString("short_summary"),
                        list(result.getString("technical_points")), result.getString("why_it_matters"),
                        result.getString("learning_priority"), list(result.getString("prerequisites")),
                        list(result.getString("related_keywords")), result.getString("importance_label")));
            }
        }
        return summaries;
    }

    private String json(List<String> values) throws SQLException {
        try { return mapper.writeValueAsString(values); } catch (Exception error) { throw new SQLException(error); }
    }

    private List<String> list(String json) throws SQLException {
        if (json == null || json.isBlank()) return List.of();
        try { return mapper.readValue(json, new TypeReference<>() { }); }
        catch (Exception error) { throw new SQLException(error); }
    }
}
