package com.example.techwatch.gui;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordMarketStatsRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.keyword.KeywordWeeklyStats;
import com.example.techwatch.market.KeywordMarketStats;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class KeywordChartDataService {
    private final Database database;
    private final KeywordWeeklyStatsRepository weekly;
    private final KeywordMarketStatsRepository market;

    public KeywordChartDataService() throws Exception {
        database = new Database(AppPaths.detect().database());
        database.initialize();
        weekly = new KeywordWeeklyStatsRepository(database);
        market = new KeywordMarketStatsRepository(database);
    }

    public List<KeywordWeeklyStats> trend(long keywordId) throws Exception { return weekly.findRecent(keywordId, 12); }
    public List<KeywordMarketStats> market(long keywordId) throws Exception { return market.findRecent(keywordId, 12); }

    public List<String> relatedArticles(long keywordId) throws Exception {
        List<String> values = new ArrayList<>();
        try (var connection = database.connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT a.title,a.url,a.article_score FROM keyword_mentions m
                JOIN articles a ON a.id=m.article_id WHERE m.keyword_id=?
                ORDER BY a.published_at DESC,a.article_score DESC LIMIT 12
                """)) {
            statement.setLong(1, keywordId);
            try (var result = statement.executeQuery()) {
                while (result.next()) values.add(String.format("%.1f点  %s%n%s", result.getDouble("article_score"),
                        result.getString("title"), result.getString("url")));
            }
        }
        return values;
    }
}
