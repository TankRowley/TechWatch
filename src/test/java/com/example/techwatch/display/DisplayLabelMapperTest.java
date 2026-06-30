package com.example.techwatch.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayLabelMapperTest {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();

    @Test
    void mapsInternalValuesAndKeepsUnknownValue() {
        assertEquals("必読", labels.articleLabel("Must Read"));
        assertEquals("流行疑い", labels.keywordStatus("Buzz"));
        assertEquals("眺めるだけ", labels.learningPriority("Watch Only"));
        assertEquals("急上昇", labels.trendState("Rising"));
        assertEquals("安定", labels.trendState("Stable"));
        assertEquals("減速", labels.trendState("Cooling"));
        assertEquals("休眠", labels.trendState("Dormant"));
        assertEquals("米国先行", labels.marketLabel("US Leading"));
        assertEquals("話題先行", labels.marketLabel("Buzz Only"));
        assertEquals("名前だけ覚える", labels.exploreJudgement("NAME_ONLY"));
        assertEquals("Future", labels.keywordStatus("Future"));
    }
}
