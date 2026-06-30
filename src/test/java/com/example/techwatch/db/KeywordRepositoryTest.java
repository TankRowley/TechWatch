package com.example.techwatch.db;

import com.example.techwatch.keyword.Keyword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordRepositoryTest {
    @TempDir Path temp;

    @Test
    void pinnedLearningAndStatusRemainIndependent() throws Exception {
        Database database = new Database(temp.resolve("preferences.db"));
        database.initialize();
        KeywordRepository repository = new KeywordRepository(database);
        Keyword keyword = repository.save(new Keyword("AI Agent", "AI", "Buzz", 4));

        repository.updatePinned(keyword.getId(), true, "継続監視");
        repository.updateLearning(keyword.getId(), true, "実装を学習中");
        Keyword saved = repository.findByNormalizedName("ai agent");

        assertTrue(saved.isPinned());
        assertTrue(saved.isLearning());
        assertTrue(saved.getPinReason().contains("継続監視"));
        assertTrue("Buzz".equals(saved.getStatus()));

        repository.updatePinned(keyword.getId(), false, "");
        Keyword unpinned = repository.findByNormalizedName("ai agent");
        assertFalse(unpinned.isPinned());
        assertTrue(unpinned.isLearning());
    }
}
