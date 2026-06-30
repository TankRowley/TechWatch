package com.example.techwatch.keyword;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordTrendEvaluatorTest {
    private final KeywordTrendEvaluator evaluator = new KeywordTrendEvaluator();

    @Test void risingWhenCurrentWeekExceedsRecentAverage() {
        assertEquals("Rising", evaluator.evaluate(history(1, 1, 1, 5)));
    }

    @Test void stableWhenPresentInThreeOfFourWeeks() {
        assertEquals("Stable", evaluator.evaluate(history(2, 2, 2, 2)));
    }

    @Test void coolingWhenRecentFourWeeksDropFromOlderHistory() {
        assertEquals("Cooling", evaluator.evaluate(history(5, 5, 5, 5, 1, 1, 1, 1)));
    }

    @Test void dormantWhenEightWeeksHaveNoMeaningfulActivity() {
        assertEquals("Dormant", evaluator.evaluate(history(0, 0, 0, 0, 0, 0, 0, 0)));
    }

    private List<KeywordWeeklyStats> history(int... mentions) {
        List<KeywordWeeklyStats> values = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 5);
        for (int i = 0; i < mentions.length; i++) {
            int count = mentions[i];
            values.add(new KeywordWeeklyStats(1, start.plusWeeks(i), count, count > 0 ? 2 : 0,
                    count > 0 ? 1 : 0, count > 0 ? 1 : 0, 0, count > 0 ? 8 : 0));
        }
        return values;
    }
}
