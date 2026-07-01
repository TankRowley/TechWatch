package com.example.techwatch.app;

import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordTrendAssessment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordPriorityServiceTest {
    @TempDir Path temp;

    @Test void keepsFoundationVisibleAndAddsOnlyPersonalPriorityForLearning() throws Exception {
        Database database = new Database(temp.resolve("priority.db")); database.initialize();
        KeywordRepository repository = new KeywordRepository(database);
        Keyword foundation = repository.save(new Keyword("Java", "Java", "Core", 5));
        Keyword specialist = repository.save(new Keyword("Kafka", "Data Engineering", "Watch", 4));
        repository.updateLearning(specialist.getId(), true, "専門領域");
        repository.updateTrendAssessment(foundation.getId(),
                new KeywordTrendAssessment("Dormant", 0, 0, 0, 80, 0, 0, 0), "Core", LocalDate.now());
        repository.updateTrendAssessment(specialist.getId(),
                new KeywordTrendAssessment("Stable", 50, 60, 0, 80, 5, 4, 3), "Watch", LocalDate.now());

        List<Keyword> result = new KeywordPriorityService(repository).evaluate(repository.findAll(), Map.of());
        Keyword updatedFoundation = result.stream().filter(Keyword::isFoundation).findFirst().orElseThrow();
        Keyword updatedSpecialist = result.stream().filter(Keyword::isLearning).findFirst().orElseThrow();

        assertTrue(updatedFoundation.getFinalScore() >= 45);
        assertTrue(updatedSpecialist.getLearningValueScore() >= 15);
        assertTrue(updatedSpecialist.getFinalScore() > updatedFoundation.getFinalScore());
    }
}
