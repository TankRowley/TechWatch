package com.example.techwatch.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class RetentionConfigLoader {
    public RetentionPolicy load(Path path) throws IOException {
        if (!Files.exists(path)) throw new IOException("データ保持設定が見つかりません: " + path);
        RetentionPolicy defaults = RetentionPolicy.defaults();
        try (InputStream in = Files.newInputStream(path)) {
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root) || !(root.get("retention") instanceof Map<?, ?> values)) {
                return defaults;
            }
            return new RetentionPolicy(
                    number(values.get("articleBodyDays"), defaults.articleBodyDays()),
                    number(values.get("rawHtmlDays"), defaults.rawHtmlDays()),
                    number(values.get("executionLogDays"), defaults.executionLogDays()),
                    number(values.get("unselectedArticleDays"), defaults.unselectedArticleDays()),
                    number(values.get("articleMetadataDays"), defaults.articleMetadataDays()),
                    number(values.get("jobSnapshotDays"), defaults.jobSnapshotDays()),
                    number(values.get("htmlReportDays"), defaults.htmlReportDays()),
                    bool(values.get("keepMarkdownReports"), defaults.keepMarkdownReports()),
                    bool(values.get("keepWeeklyKeywordStats"), defaults.keepWeeklyKeywordStats()),
                    bool(values.get("keepKeywordMarketStats"), defaults.keepKeywordMarketStats()));
        }
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number number) return Math.max(0, number.intValue());
        try { return Math.max(0, Integer.parseInt(String.valueOf(value))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private boolean bool(Object value, boolean fallback) {
        if (value instanceof Boolean bool) return bool;
        if (value == null) return fallback;
        String text = value.toString().trim();
        return "true".equalsIgnoreCase(text) ? true : "false".equalsIgnoreCase(text) ? false : fallback;
    }
}
