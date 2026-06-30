package com.example.techwatch.db;

import com.example.techwatch.market.KeywordMarketStats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeywordMarketStatsRepository {
    private final Database database;
    public KeywordMarketStatsRepository(Database database) { this.database = database; }

    public void save(KeywordMarketStats value) throws SQLException {
        String now = Instant.now().toString();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO keyword_market_stats(keyword_id,week_start,us_job_count,jp_job_count,us_growth_4w,
                  jp_growth_4w,us_growth_12w,jp_growth_12w,us_market_score,jp_market_score,
                  global_market_score,market_label,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(keyword_id,week_start) DO UPDATE SET us_job_count=excluded.us_job_count,
                  jp_job_count=excluded.jp_job_count,us_growth_4w=excluded.us_growth_4w,
                  jp_growth_4w=excluded.jp_growth_4w,us_growth_12w=excluded.us_growth_12w,
                  jp_growth_12w=excluded.jp_growth_12w,us_market_score=excluded.us_market_score,
                  jp_market_score=excluded.jp_market_score,global_market_score=excluded.global_market_score,
                  market_label=excluded.market_label,updated_at=excluded.updated_at
                """)) {
            int i = 1;
            statement.setLong(i++, value.keywordId()); statement.setString(i++, value.weekStart().toString());
            statement.setInt(i++, value.usJobCount()); statement.setInt(i++, value.jpJobCount());
            statement.setDouble(i++, value.usGrowth4w()); statement.setDouble(i++, value.jpGrowth4w());
            statement.setDouble(i++, value.usGrowth12w()); statement.setDouble(i++, value.jpGrowth12w());
            statement.setDouble(i++, value.usMarketScore()); statement.setDouble(i++, value.jpMarketScore());
            statement.setDouble(i++, value.globalMarketScore()); statement.setString(i++, value.marketLabel());
            statement.setString(i++, now); statement.setString(i, now); statement.executeUpdate();
        }
    }

    public Map<Long, KeywordMarketStats> findLatestByKeyword() throws SQLException {
        Map<Long, KeywordMarketStats> values = new LinkedHashMap<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT s.* FROM keyword_market_stats s JOIN (
                  SELECT keyword_id,MAX(week_start) week_start FROM keyword_market_stats GROUP BY keyword_id
                ) latest ON latest.keyword_id=s.keyword_id AND latest.week_start=s.week_start
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) values.put(result.getLong("keyword_id"), map(result));
        }
        return values;
    }

    public List<KeywordMarketStats> findRecent(long keywordId, int weeks) throws SQLException {
        List<KeywordMarketStats> values = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM (SELECT * FROM keyword_market_stats WHERE keyword_id=?
                  ORDER BY week_start DESC LIMIT ?) ORDER BY week_start
                """)) {
            statement.setLong(1, keywordId); statement.setInt(2, weeks);
            try (ResultSet result = statement.executeQuery()) { while (result.next()) values.add(map(result)); }
        }
        return values;
    }

    private KeywordMarketStats map(ResultSet result) throws SQLException {
        return new KeywordMarketStats(result.getLong("keyword_id"), LocalDate.parse(result.getString("week_start")),
                result.getInt("us_job_count"), result.getInt("jp_job_count"),
                result.getDouble("us_growth_4w"), result.getDouble("jp_growth_4w"),
                result.getDouble("us_growth_12w"), result.getDouble("jp_growth_12w"),
                result.getDouble("us_market_score"), result.getDouble("jp_market_score"),
                result.getDouble("global_market_score"), result.getString("market_label"));
    }
}
