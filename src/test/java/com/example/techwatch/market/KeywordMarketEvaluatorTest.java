package com.example.techwatch.market;

import com.example.techwatch.keyword.Keyword;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordMarketEvaluatorTest {
    @Test void identifiesUsLeadingDemand() {
        Keyword keyword = new Keyword(1L, "Databricks", "databricks", "Data Engineering", "Watch", 4,
                3, 2, 0, 0, 0, 0, null, null);
        KeywordMarketStats result = new KeywordMarketEvaluator().evaluate(keyword, LocalDate.of(2026, 6, 29),
                1800, 120, List.of());
        assertEquals("US Leading", result.marketLabel());
        assertTrue(result.globalMarketScore() > 0);
    }
    @Test void firstObservationHasNoFakeGrowth(){
        Keyword k=new Keyword(1L,"New Tool","new tool","Other","Watch",3,0,0,0,0,0,0,null,null);
        var r=new KeywordMarketEvaluator().evaluate(k,LocalDate.of(2026,6,29),100,20,List.of());
        assertEquals(0,r.usGrowth4w()); assertEquals("OBSERVED",r.observationStatus());
    }
}
