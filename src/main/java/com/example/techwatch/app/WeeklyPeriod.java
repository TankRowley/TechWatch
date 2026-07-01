package com.example.techwatch.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record WeeklyPeriod(LocalDate start, LocalDate end) {
    public static WeeklyPeriod previousCompleted(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today is required");
        LocalDate currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = currentMonday.minusDays(1);
        return new WeeklyPeriod(end.minusDays(6), end);
    }
}
