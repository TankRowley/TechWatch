package com.example.techwatch.keyword;

import com.example.techwatch.db.KeywordStats;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordEvaluatorTest {
    @Test
    void protectsCoreTechnology() {
        Keyword java = new Keyword("Java", "Java", "Core", 5);
        var result = new KeywordEvaluator().evaluate(java, new KeywordStats(1, 0, 0, 0, 0, 0), Instant.now());
        assertEquals("Core", result.status());
    }

    @Test
    void detectsSourceBiasedSpikeWithoutChangingStatusBeforeHistoryIsAvailable() {
        Keyword candidate = new Keyword("Shiny Tool", "AI", "Candidate", 3);
        var result = new KeywordEvaluator().evaluate(candidate, new KeywordStats(1, 8, 1, 1, 1, 7), Instant.now());
        assertEquals("Candidate", result.status());
        assertTrue(result.buzzRiskScore() > 0);
    }
}
