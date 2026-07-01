package com.example.techwatch.keyword;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test void normalizesMentionsByCollectedArticleVolume() {
        var lowRate = assessed(100, 5, 5, 5, 5);
        var highRate = assessed(10, 5, 5, 5, 5);

        assertTrue(highRate.activityScore() > lowRate.activityScore());
    }

    @Test void missingCollectionsDoNotBecomeZeroDemand() {
        List<KeywordWeeklyStats> values = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 5);
        for (int i = 0; i < 8; i++) values.add(new KeywordWeeklyStats(1, start.plusWeeks(i), 0, 0,
                0, 0, 0, 0, 0, 0, 2, "MISSING", 1));

        assertEquals("Insufficient", evaluator.evaluate(values));
    }
    @Test void historicalPartialHasReducedCoverage(){
        var v=new KeywordWeeklyStats(1,LocalDate.of(2026,1,5),3,1,1,1,0,8,20,1,4,"HISTORICAL_PARTIAL",1);
        assertEquals(0.25,v.collectionCoverage());
    }

    private List<KeywordWeeklyStats> history(int... mentions) {
        List<KeywordWeeklyStats> values = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 5);
        for (int i = 0; i < mentions.length; i++) {
            int count = mentions[i];
            values.add(new KeywordWeeklyStats(1, start.plusWeeks(i), count, count > 0 ? 2 : 0,
                    count > 0 ? 1 : 0, count > 0 ? 1 : 0, 0, count > 0 ? 8 : 0,
                    20, 2, 2, "SUCCESS", count > 0 ? 0.5 : 1));
        }
        return values;
    }

    private KeywordTrendAssessment assessed(int totalArticles, int... mentions) {
        List<KeywordWeeklyStats> values = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 5);
        for (int i = 0; i < mentions.length; i++) {
            int count = mentions[i];
            values.add(new KeywordWeeklyStats(1, start.plusWeeks(i), count, 2, count, count, 0, 8,
                    totalArticles, 2, 2, "SUCCESS", 0.5));
        }
        return evaluator.assess(values);
    }
}
