package com.example.techwatch.app;

import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordTrendEvaluator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordHistoryServiceTest {
    @TempDir Path temp;

    @Test void coreLearningAndPinnedAreNotAutomaticallyIgnored() throws Exception {
        Database database = new Database(temp.resolve("history.db")); database.initialize();
        KeywordRepository repository = new KeywordRepository(database);
        Keyword core = repository.save(new Keyword("Java", "Backend", "Core", 5));
        Keyword learning = repository.save(new Keyword("SQL", "Backend", "Watch", 4));
        Keyword pinned = repository.save(new Keyword("Databricks", "Data Engineering", "Watch", 4));
        Keyword ordinary = repository.save(new Keyword("Tool X", "Development Tools", "Watch", 2));
        repository.updateLearning(learning.getId(), true, "学習中");
        repository.updatePinned(pinned.getId(), true, "固定中");

        new KeywordHistoryService(repository, new KeywordWeeklyStatsRepository(database),
                new KeywordTrendEvaluator()).captureAndEvaluate(LocalDate.of(2026, 6, 29));

        assertEquals("Core", repository.findByNormalizedName(core.getNormalizedName()).getStatus());
        assertEquals("Watch", repository.findByNormalizedName(learning.getNormalizedName()).getStatus());
        assertEquals("Watch", repository.findByNormalizedName(pinned.getNormalizedName()).getStatus());
        assertEquals("Watch", repository.findByNormalizedName(ordinary.getNormalizedName()).getStatus());

        var service = new KeywordHistoryService(repository, new KeywordWeeklyStatsRepository(database),
                new KeywordTrendEvaluator());
        service.captureAndEvaluate(LocalDate.of(2026, 6, 29));
        assertEquals("Watch", repository.findByNormalizedName(ordinary.getNormalizedName()).getStatus());

        service.captureAndEvaluate(LocalDate.of(2026, 7, 27));
        assertEquals("Watch", repository.findByNormalizedName(ordinary.getNormalizedName()).getStatus());
    }
}
