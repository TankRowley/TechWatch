package com.example.techwatch.db;

import java.time.Instant;

final class DbTime {
    private DbTime() { }

    static String text(Instant instant) { return instant == null ? null : instant.toString(); }

    static Instant instant(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Instant.parse(value); } catch (RuntimeException ignored) { return null; }
    }
}
