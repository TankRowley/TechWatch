package com.example.techwatch.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String jdbcUrl;

    public Database(Path path) {
        this.jdbcUrl = "jdbc:sqlite:" + path.toAbsolutePath().normalize();
    }

    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sources (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL UNIQUE,
                        type TEXT NOT NULL,
                        trust_score INTEGER NOT NULL DEFAULT 1,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS articles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        source_id INTEGER,
                        title TEXT NOT NULL,
                        url TEXT NOT NULL UNIQUE,
                        published_at TEXT,
                        fetched_at TEXT NOT NULL,
                        summary_original TEXT,
                        body_status TEXT NOT NULL DEFAULT 'SKIPPED',
                        article_score REAL NOT NULL DEFAULT 0,
                        importance_label TEXT NOT NULL DEFAULT 'UNRATED',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (source_id) REFERENCES sources(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS article_summaries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        article_id INTEGER NOT NULL UNIQUE,
                        short_summary TEXT,
                        technical_points TEXT,
                        why_it_matters TEXT,
                        learning_priority TEXT,
                        prerequisites TEXT,
                        related_keywords TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS keywords (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        normalized_name TEXT NOT NULL UNIQUE,
                        category TEXT,
                        status TEXT NOT NULL DEFAULT 'Candidate',
                        weight INTEGER NOT NULL DEFAULT 1,
                        trend_score REAL NOT NULL DEFAULT 0,
                        stability_score REAL NOT NULL DEFAULT 0,
                        market_score REAL NOT NULL DEFAULT 0,
                        learning_value_score REAL NOT NULL DEFAULT 0,
                        buzz_risk_score REAL NOT NULL DEFAULT 0,
                        final_score REAL NOT NULL DEFAULT 0,
                        first_seen_at TEXT,
                        last_seen_at TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS keyword_mentions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        article_id INTEGER NOT NULL,
                        keyword_id INTEGER NOT NULL,
                        detected_in TEXT NOT NULL,
                        observed_at TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
                        FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE,
                        UNIQUE(article_id, keyword_id, detected_in)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS weekly_reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_date TEXT NOT NULL UNIQUE,
                        file_path TEXT NOT NULL,
                        article_count INTEGER NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS report_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        report_id INTEGER NOT NULL,
                        article_id INTEGER NOT NULL,
                        rank INTEGER NOT NULL,
                        reason TEXT,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE CASCADE,
                        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
                        UNIQUE(report_id, article_id)
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_articles_score ON articles(article_score DESC)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_articles_published ON articles(published_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mentions_observed ON keyword_mentions(observed_at)");
        }
    }
}
