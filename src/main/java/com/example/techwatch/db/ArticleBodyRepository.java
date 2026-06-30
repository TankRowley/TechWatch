package com.example.techwatch.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public class ArticleBodyRepository {
    private final Database database;

    public ArticleBodyRepository(Database database) { this.database = database; }

    public void save(long articleId, String bodyText, String rawHtml, Instant fetchedAt) throws SQLException {
        if (blank(bodyText) && blank(rawHtml)) return;
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO article_bodies(article_id,body_text,raw_html,fetched_at,created_at,updated_at)
                VALUES(?,?,?,?,?,?)
                ON CONFLICT(article_id) DO UPDATE SET
                  body_text=COALESCE(excluded.body_text,article_bodies.body_text),
                  raw_html=COALESCE(excluded.raw_html,article_bodies.raw_html),
                  fetched_at=excluded.fetched_at,updated_at=excluded.updated_at
                """)) {
            statement.setLong(1, articleId);
            nullableText(statement, 2, bodyText);
            nullableText(statement, 3, rawHtml);
            statement.setString(4, (fetchedAt == null ? Instant.now() : fetchedAt).toString());
            statement.setString(5, now);
            statement.setString(6, now);
            statement.executeUpdate();
        }
    }

    public Optional<String> findBodyText(long articleId) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT body_text FROM article_bodies WHERE article_id=?")) {
            statement.setLong(1, articleId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.ofNullable(result.getString(1)) : Optional.empty();
            }
        }
    }

    private void nullableText(PreparedStatement statement, int index, String value) throws SQLException {
        if (blank(value)) statement.setNull(index, java.sql.Types.VARCHAR); else statement.setString(index, value);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
