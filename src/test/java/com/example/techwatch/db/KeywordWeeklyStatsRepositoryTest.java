package com.example.techwatch.db;

import com.example.techwatch.keyword.KeywordWeeklyStats;
import com.example.techwatch.keyword.Keyword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordWeeklyStatsRepositoryTest {
    @TempDir Path temp;

    @Test void savesAndReadsTwelveWeekChartHistory() throws Exception {
        Database database = new Database(temp.resolve("stats.db")); database.initialize();
        KeywordWeeklyStatsRepository repository = new KeywordWeeklyStatsRepository(database);
        long keywordId = new KeywordRepository(database).save(new Keyword("Java", "Backend", "Core", 5)).getId();
        for (int i = 0; i < 14; i++) repository.save(new KeywordWeeklyStats(keywordId, LocalDate.of(2026, 1, 5).plusWeeks(i),
                i, 2, 1, 1, 0, 7.5));

        var values = repository.findRecent(keywordId, 12);

        assertEquals(12, values.size());
        assertEquals(2, values.get(0).mentionCount());
        assertEquals(13, values.get(11).mentionCount());
    }
}
