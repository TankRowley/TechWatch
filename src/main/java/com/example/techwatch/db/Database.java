package com.example.techwatch.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

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
                        archived INTEGER NOT NULL DEFAULT 0,
                        saved_by_user INTEGER NOT NULL DEFAULT 0,
                        cleanup_protected INTEGER NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (source_id) REFERENCES sources(id)
                    )
                    """);
            ensureColumn(connection, "articles", "archived", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "articles", "saved_by_user", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "articles", "cleanup_protected", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "articles", "processing_status", "TEXT NOT NULL DEFAULT 'PENDING'");
            ensureColumn(connection, "articles", "processing_attempts", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "articles", "last_processing_error", "TEXT");
            ensureColumn(connection, "articles", "last_processing_at", "TEXT");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS article_bodies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        article_id INTEGER NOT NULL UNIQUE,
                        body_text TEXT,
                        raw_html TEXT,
                        fetched_at TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
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
                        pinned INTEGER NOT NULL DEFAULT 0,
                        pinned_at TEXT,
                        pin_reason TEXT,
                        learning INTEGER NOT NULL DEFAULT 0,
                        learning_since TEXT,
                        learning_reason TEXT,
                        trend_state TEXT NOT NULL DEFAULT 'Dormant',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            ensureColumn(connection, "keywords", "pinned", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "keywords", "pinned_at", "TEXT");
            ensureColumn(connection, "keywords", "pin_reason", "TEXT");
            ensureColumn(connection, "keywords", "learning", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "keywords", "learning_since", "TEXT");
            ensureColumn(connection, "keywords", "learning_reason", "TEXT");
            ensureColumn(connection, "keywords", "trend_state", "TEXT NOT NULL DEFAULT 'Dormant'");
            ensureColumn(connection, "keywords", "status_changed_week", "TEXT");
            ensureColumn(connection, "keywords", "activity_score", "REAL NOT NULL DEFAULT 0");
            ensureColumn(connection, "keywords", "confidence_score", "REAL NOT NULL DEFAULT 0");
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
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        display_name TEXT,
                        primary_goal TEXT,
                        experience_level TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_interest_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        category TEXT NOT NULL UNIQUE,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_articles_score ON articles(article_score DESC)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_articles_published ON articles(published_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_article_bodies_fetched ON article_bodies(fetched_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mentions_observed ON keyword_mentions(observed_at)");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS keyword_weekly_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        keyword_id INTEGER NOT NULL,
                        week_start TEXT NOT NULL,
                        mention_count INTEGER NOT NULL DEFAULT 0,
                        source_count INTEGER NOT NULL DEFAULT 0,
                        official_source_count INTEGER NOT NULL DEFAULT 0,
                        high_score_article_count INTEGER NOT NULL DEFAULT 0,
                        report_included_count INTEGER NOT NULL DEFAULT 0,
                        average_article_score REAL NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE,
                        UNIQUE(keyword_id, week_start)
                    )
                    """);
            ensureColumn(connection, "keyword_weekly_stats", "total_article_count", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "keyword_weekly_stats", "successful_source_count", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "keyword_weekly_stats", "configured_source_count", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(connection, "keyword_weekly_stats", "collection_status", "TEXT NOT NULL DEFAULT 'LEGACY'");
            ensureColumn(connection, "keyword_weekly_stats", "source_concentration", "REAL NOT NULL DEFAULT 1");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS discovered_keywords (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        normalized_name TEXT NOT NULL UNIQUE,
                        category TEXT,
                        description TEXT,
                        learning_judgement TEXT NOT NULL DEFAULT 'UNKNOWN',
                        prerequisites TEXT,
                        first_seen_at TEXT,
                        last_seen_at TEXT,
                        mention_count INTEGER NOT NULL DEFAULT 0,
                        promoted_to_keyword INTEGER NOT NULL DEFAULT 0,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS discovered_keyword_mentions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        discovered_keyword_id INTEGER NOT NULL,
                        article_id INTEGER NOT NULL,
                        detected_in TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (discovered_keyword_id) REFERENCES discovered_keywords(id) ON DELETE CASCADE,
                        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
                        UNIQUE(discovered_keyword_id, article_id, detected_in)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS job_market_snapshots (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        keyword_id INTEGER NOT NULL,
                        region TEXT NOT NULL,
                        source_name TEXT NOT NULL,
                        query TEXT NOT NULL,
                        job_count INTEGER NOT NULL DEFAULT 0,
                        salary_min INTEGER,
                        salary_max INTEGER,
                        salary_median INTEGER,
                        fetched_at TEXT NOT NULL,
                        week_start TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE,
                        UNIQUE(keyword_id, region, source_name, query, week_start)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS keyword_market_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        keyword_id INTEGER NOT NULL,
                        week_start TEXT NOT NULL,
                        us_job_count INTEGER NOT NULL DEFAULT 0,
                        jp_job_count INTEGER NOT NULL DEFAULT 0,
                        us_growth_4w REAL NOT NULL DEFAULT 0,
                        jp_growth_4w REAL NOT NULL DEFAULT 0,
                        us_growth_12w REAL NOT NULL DEFAULT 0,
                        jp_growth_12w REAL NOT NULL DEFAULT 0,
                        us_market_score REAL NOT NULL DEFAULT 0,
                        jp_market_score REAL NOT NULL DEFAULT 0,
                        global_market_score REAL NOT NULL DEFAULT 0,
                        market_label TEXT NOT NULL DEFAULT 'Unknown',
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL,
                        FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE,
                        UNIQUE(keyword_id, week_start)
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_keyword_weekly_stats ON keyword_weekly_stats(keyword_id,week_start)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_keyword_market_stats ON keyword_market_stats(keyword_id,week_start)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_discovered_last_seen ON discovered_keywords(last_seen_at DESC)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_job_snapshots_fetched ON job_market_snapshots(fetched_at)");
            statement.executeUpdate("""
                    UPDATE keyword_weekly_stats
                    SET total_article_count=(
                      SELECT COUNT(*) FROM articles a
                      WHERE date(COALESCE(a.published_at,a.fetched_at))>=date(keyword_weekly_stats.week_start)
                        AND date(COALESCE(a.published_at,a.fetched_at))<date(keyword_weekly_stats.week_start,'+7 days')
                    )
                    WHERE total_article_count=0
                    """);
            statement.executeUpdate("""
                    UPDATE articles SET processing_status='COMPLETE'
                    WHERE processing_status='PENDING'
                      AND processing_attempts=0 AND last_processing_at IS NULL
                      AND EXISTS (SELECT 1 FROM article_summaries s WHERE s.article_id=articles.id)
                    """);
        }
    }

    private void ensureColumn(Connection connection, String table, String column, String definition) throws SQLException {
        boolean exists = false;
        try (Statement check = connection.createStatement(); ResultSet result = check.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) { exists = true; break; }
            }
        }
        if (!exists) {
            try (Statement alter = connection.createStatement()) {
                alter.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            }
        }
    }
}
