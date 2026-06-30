package com.example.techwatch.app;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSetupServiceTest {
    @TempDir Path temp;

    @Test
    void savesLearningPinnedAndInterestSelections() throws Exception {
        AppPaths paths = new AppPaths(temp);
        UserSetupService service = new UserSetupService(paths);
        service.save(new UserSetupSelection("Hiro", "Javaからクラウドへ", Set.of("Java", "Docker"),
                Set.of("MCP"), Set.of("Backend", "Cloud")));

        assertTrue(service.isSetupComplete());
        assertEquals(Set.of("Backend", "Cloud"), service.interestCategories());
        KeywordRepository keywords = new KeywordRepository(new Database(paths.database()));
        assertTrue(keywords.findByNormalizedName("java").isLearning());
        assertTrue(keywords.findByNormalizedName("mcp").isPinned());
    }
}
