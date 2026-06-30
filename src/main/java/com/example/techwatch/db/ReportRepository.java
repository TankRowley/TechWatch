package com.example.techwatch.db;

import com.example.techwatch.article.Article;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ReportRepository {
    private final Database database;

    public ReportRepository(Database database) { this.database = database; }

    public void save(LocalDate reportDate, Path path, List<Article> articles) throws SQLException {
        try (var connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                String now = Instant.now().toString();
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO weekly_reports(report_date,file_path,article_count,created_at) VALUES(?,?,?,?)
                        ON CONFLICT(report_date) DO UPDATE SET file_path=excluded.file_path,
                          article_count=excluded.article_count,created_at=excluded.created_at
                        """)) {
                    statement.setString(1, reportDate.toString()); statement.setString(2, path.toString());
                    statement.setInt(3, articles.size()); statement.setString(4, now); statement.executeUpdate();
                }
                long reportId;
                try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM weekly_reports WHERE report_date=?")) {
                    statement.setString(1, reportDate.toString());
                    try (ResultSet result = statement.executeQuery()) { result.next(); reportId = result.getLong(1); }
                }
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM report_items WHERE report_id=?")) {
                    delete.setLong(1, reportId); delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO report_items(report_id,article_id,rank,reason,created_at) VALUES(?,?,?,?,?)
                        """)) {
                    int rank = 1;
                    for (Article article : articles) {
                        if (article.getId() == null || "Ignore".equals(article.getImportanceLabel())) continue;
                        insert.setLong(1, reportId); insert.setLong(2, article.getId()); insert.setInt(3, rank++);
                        insert.setString(4, article.getImportanceLabel()); insert.setString(5, now); insert.addBatch();
                    }
                    insert.executeBatch();
                }
                connection.commit();
            } catch (Exception error) {
                connection.rollback();
                if (error instanceof SQLException sql) throw sql;
                throw new SQLException(error);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
