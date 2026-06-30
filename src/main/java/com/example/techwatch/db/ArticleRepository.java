package com.example.techwatch.db;

import com.example.techwatch.article.Article;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ArticleRepository {
    private static final String SELECT_COLUMNS = """
            SELECT a.*, COALESCE(s.name, 'Unknown source') AS source_name
            FROM articles a LEFT JOIN sources s ON s.id=a.source_id
            """;
    private final Database database;

    public ArticleRepository(Database database) { this.database = database; }

    public Optional<Article> save(Article article) throws SQLException {
        String now = Instant.now().toString();
        String sql = """
                INSERT OR IGNORE INTO articles(source_id,title,url,published_at,fetched_at,summary_original,
                  body_status,article_score,importance_label,created_at,updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (var connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (article.getSourceId() == null) statement.setNull(1, java.sql.Types.INTEGER);
            else statement.setLong(1, article.getSourceId());
            statement.setString(2, article.getTitle());
            statement.setString(3, article.getUrl());
            statement.setString(4, DbTime.text(article.getPublishedAt()));
            statement.setString(5, DbTime.text(article.getFetchedAt()));
            statement.setString(6, article.getSummaryOriginal());
            statement.setString(7, article.getBodyStatus());
            statement.setDouble(8, article.getArticleScore());
            statement.setString(9, article.getImportanceLabel());
            statement.setString(10, now);
            statement.setString(11, now);
            if (statement.executeUpdate() == 0) return Optional.empty();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) article.setId(keys.getLong(1));
            }
            return Optional.of(article);
        }
    }

    public void updateAnalysis(long articleId, double score, String label, String bodyStatus) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE articles SET article_score=?, importance_label=?, body_status=?, updated_at=? WHERE id=?
                """)) {
            statement.setDouble(1, score);
            statement.setString(2, label);
            statement.setString(3, bodyStatus);
            statement.setString(4, Instant.now().toString());
            statement.setLong(5, articleId);
            statement.executeUpdate();
        }
    }

    public void setSavedByUser(long articleId, boolean saved) throws SQLException {
        updateFlag(articleId, "saved_by_user", saved);
    }

    public void setCleanupProtected(long articleId, boolean protectedFromCleanup) throws SQLException {
        updateFlag(articleId, "cleanup_protected", protectedFromCleanup);
    }

    private void updateFlag(long articleId, String column, boolean value) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(
                "UPDATE articles SET " + column + "=?,updated_at=? WHERE id=?")) {
            statement.setInt(1, value ? 1 : 0);
            statement.setString(2, Instant.now().toString());
            statement.setLong(3, articleId);
            statement.executeUpdate();
        }
    }

    public Optional<Article> findByUrl(String url) throws SQLException {
        return findOne(SELECT_COLUMNS + " WHERE a.url=?", url);
    }

    public Optional<Article> findById(long id) throws SQLException {
        return findOne(SELECT_COLUMNS + " WHERE a.id=?", id);
    }

    private Optional<Article> findOne(String sql, Object value) throws SQLException {
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, value);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public List<Article> findAllByScore() throws SQLException {
        return query(SELECT_COLUMNS + " ORDER BY a.article_score DESC, COALESCE(a.published_at,a.fetched_at) DESC", null, null);
    }

    public List<Article> findBetween(Instant startInclusive, Instant endExclusive) throws SQLException {
        return query(SELECT_COLUMNS + " WHERE COALESCE(a.published_at,a.fetched_at)>=? AND COALESCE(a.published_at,a.fetched_at)<? "
                + "ORDER BY a.article_score DESC, COALESCE(a.published_at,a.fetched_at) DESC", startInclusive, endExclusive);
    }

    private List<Article> query(String sql, Instant start, Instant end) throws SQLException {
        List<Article> articles = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (start != null) {
                statement.setString(1, start.toString());
                statement.setString(2, end.toString());
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) articles.add(map(result));
            }
        }
        return articles;
    }

    private Article map(ResultSet result) throws SQLException {
        return new Article(result.getLong("id"), nullableLong(result, "source_id"), result.getString("source_name"),
                result.getString("title"), result.getString("url"), DbTime.instant(result.getString("published_at")),
                DbTime.instant(result.getString("fetched_at")), result.getString("summary_original"),
                result.getString("body_status"), result.getDouble("article_score"), result.getString("importance_label"),
                result.getInt("archived") != 0, result.getInt("saved_by_user") != 0,
                result.getInt("cleanup_protected") != 0);
    }

    private Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }
}
