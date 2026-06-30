package com.example.techwatch.source;

import java.util.Objects;

public record Source(Long id, String name, String url, String type, int trustScore, String status) {
    public Source {
        name = Objects.requireNonNullElse(name, "Unknown source").trim();
        url = Objects.requireNonNullElse(url, "").trim();
        type = Objects.requireNonNullElse(type, "rss").trim();
        trustScore = Math.max(1, Math.min(5, trustScore));
        status = Objects.requireNonNullElse(status, "ACTIVE").trim();
    }

    public Source(String name, String url, String type, int trustScore) {
        this(null, name, url, type, trustScore, "ACTIVE");
    }

    public Source withId(long newId) {
        return new Source(newId, name, url, type, trustScore, status);
    }
}
