package com.example.techwatch.db;

import com.example.techwatch.explore.DiscoveredKeyword;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiscoveredKeywordRepository {
    private final Database database;
    private final ObjectMapper mapper = new ObjectMapper();

    public DiscoveredKeywordRepository(Database database) { this.database = database; }

    public DiscoveredKeyword saveMention(DiscoveredKeyword keyword, long articleId, String detectedIn) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO discovered_keywords(name,normalized_name,category,description,learning_judgement,
                  prerequisites,first_seen_at,last_seen_at,mention_count,promoted_to_keyword,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?,?,0,0,?,?)
                ON CONFLICT(normalized_name) DO UPDATE SET category=excluded.category,
                  description=CASE WHEN discovered_keywords.description='' THEN excluded.description ELSE discovered_keywords.description END,
                  last_seen_at=excluded.last_seen_at,updated_at=excluded.updated_at
                """)) {
            statement.setString(1, keyword.name()); statement.setString(2, keyword.normalizedName());
            statement.setString(3, keyword.category()); statement.setString(4, keyword.description());
            statement.setString(5, keyword.learningJudgement()); statement.setString(6, json(keyword.prerequisites()));
            statement.setString(7, now); statement.setString(8, now); statement.setString(9, now);
            statement.setString(10, now); statement.executeUpdate();
        }
        DiscoveredKeyword saved = findByNormalizedName(keyword.normalizedName());
        boolean inserted;
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO discovered_keyword_mentions(discovered_keyword_id,article_id,detected_in,created_at)
                VALUES(?,?,?,?)
                """)) {
            statement.setLong(1, saved.id()); statement.setLong(2, articleId);
            statement.setString(3, detectedIn); statement.setString(4, now);
            inserted = statement.executeUpdate() > 0;
        }
        if (inserted) {
            try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                    "UPDATE discovered_keywords SET mention_count=mention_count+1,updated_at=? WHERE id=?")) {
                statement.setString(1, now); statement.setLong(2, saved.id()); statement.executeUpdate();
            }
        }
        return findByNormalizedName(keyword.normalizedName());
    }

    public List<DiscoveredKeyword> findAllActive() throws SQLException {
        List<DiscoveredKeyword> values = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM discovered_keywords WHERE promoted_to_keyword=0
                ORDER BY last_seen_at DESC,mention_count DESC,name
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) values.add(map(result));
        }
        return values;
    }

    public void updateJudgement(long id, String judgement) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE discovered_keywords SET learning_judgement=?,updated_at=? WHERE id=?")) {
            statement.setString(1, judgement); statement.setString(2, Instant.now().toString());
            statement.setLong(3, id); statement.executeUpdate();
        }
    }

    public void markPromoted(long id) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE discovered_keywords SET promoted_to_keyword=1,updated_at=? WHERE id=?")) {
            statement.setString(1, Instant.now().toString()); statement.setLong(2, id); statement.executeUpdate();
        }
    }

    private DiscoveredKeyword findByNormalizedName(String name) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM discovered_keywords WHERE normalized_name=?")) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("未知キーワードを保存できませんでした: " + name);
                return map(result);
            }
        }
    }

    private DiscoveredKeyword map(ResultSet result) throws SQLException {
        return new DiscoveredKeyword(result.getLong("id"), result.getString("name"),
                result.getString("normalized_name"), result.getString("category"),
                result.getString("description"), result.getString("learning_judgement"),
                list(result.getString("prerequisites")), DbTime.instant(result.getString("first_seen_at")),
                DbTime.instant(result.getString("last_seen_at")), result.getInt("mention_count"),
                result.getInt("promoted_to_keyword") == 1);
    }

    private String json(List<String> value) throws SQLException {
        try { return mapper.writeValueAsString(value); } catch (Exception error) { throw new SQLException(error); }
    }

    private List<String> list(String value) throws SQLException {
        if (value == null || value.isBlank()) return List.of();
        try { return mapper.readValue(value, new TypeReference<>() { }); }
        catch (Exception error) { throw new SQLException(error); }
    }
}
