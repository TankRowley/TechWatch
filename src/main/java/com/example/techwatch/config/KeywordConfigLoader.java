package com.example.techwatch.config;

import com.example.techwatch.keyword.Keyword;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeywordConfigLoader {
    public List<Keyword> load(Path path) throws IOException {
        if (!Files.exists(path)) throw new IOException("キーワード設定が見つかりません: " + path);
        try (InputStream in = Files.newInputStream(path)) {
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root)) return List.of();
            Object values = root.get("keywords");
            if (!(values instanceof List<?> list)) return List.of();
            List<Keyword> result = new ArrayList<>();
            for (Object value : list) {
                if (!(value instanceof Map<?, ?> item)) continue;
                String name = text(item.get("name"));
                if (name.isBlank()) continue;
                result.add(new Keyword(name, defaultText(item.get("category"), "Other"),
                        defaultText(item.get("status"), "Candidate"), number(item.get("weight"), 1)));
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
