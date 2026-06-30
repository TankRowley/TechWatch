package com.example.techwatch.market;

import java.time.Instant;
import java.time.LocalDate;

public record JobMarketSnapshot(long keywordId, String region, String sourceName, String query,
                                int jobCount, Integer salaryMin, Integer salaryMax, Integer salaryMedian,
                                Instant fetchedAt, LocalDate weekStart) { }
