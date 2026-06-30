package com.example.techwatch.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationTest {
    @TempDir Path temp;

    @Test
    void addsPreferenceColumnsToExistingKeywordTable() throws Exception {
        Path path = temp.resolve("old.db");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE keywords (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,
                      normalized_name TEXT NOT NULL UNIQUE,category TEXT,status TEXT,weight INTEGER,
                      trend_score REAL,stability_score REAL,market_score REAL,learning_value_score REAL,
                      buzz_risk_score REAL,final_score REAL,first_seen_at TEXT,last_seen_at TEXT,
                      created_at TEXT NOT NULL,updated_at TEXT NOT NULL)
                    """);
        }

        new Database(path).initialize();

        Set<String> columns = new HashSet<>();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement(); var result = statement.executeQuery("PRAGMA table_info(keywords)")) {
            while (result.next()) columns.add(result.getString("name"));
        }
        assertTrue(columns.containsAll(Set.of("pinned", "pinned_at", "pin_reason", "learning", "learning_since",
                "learning_reason", "trend_state")));
        Set<String> tables = new HashSet<>();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            while (result.next()) tables.add(result.getString("name"));
        }
        assertTrue(tables.containsAll(Set.of("article_bodies", "keyword_weekly_stats", "discovered_keywords",
                "discovered_keyword_mentions", "job_market_snapshots", "keyword_market_stats")));

        Set<String> articleColumns = new HashSet<>();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path);
             var statement = connection.createStatement(); var result = statement.executeQuery("PRAGMA table_info(articles)")) {
            while (result.next()) articleColumns.add(result.getString("name"));
        }
        assertTrue(articleColumns.containsAll(Set.of("archived", "saved_by_user", "cleanup_protected")));
    }
}
