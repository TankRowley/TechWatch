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
        assertEquals("Future", labels.keywordStatus("Future"));
    }
}
