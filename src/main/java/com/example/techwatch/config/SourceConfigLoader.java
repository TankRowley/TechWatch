package com.example.techwatch.config;

import com.example.techwatch.source.Source;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SourceConfigLoader {
    public List<Source> load(Path path) throws IOException {
        if (!Files.exists(path)) throw new IOException("情報源設定が見つかりません: " + path);
        try (InputStream in = Files.newInputStream(path)) {
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root)) return List.of();
            Object values = root.get("sources");
            if (!(values instanceof List<?> list)) return List.of();
            List<Source> result = new ArrayList<>();
            for (Object value : list) {
                if (!(value instanceof Map<?, ?> item)) continue;
                String name = text(item.get("name"));
                String url = text(item.get("url"));
                if (name.isBlank() || url.isBlank()) continue;
                result.add(new Source(name, url, defaultText(item.get("type"), "rss"),
                        number(item.get("trustScore"), 1), defaultText(item.get("category"), "OTHER")));
            }
            return List.copyOf(result);
        }
    }

    private static String text(Object value) { return value == null ? "" : value.toString().trim(); }
    private static String defaultText(Object value, String fallback) { String text = text(value); return text.isBlank() ? fallback : text; }
    private static int number(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(text(value)); } catch (NumberFormatException ignored) { return fallback; }
    }
}
