package com.example.techwatch.market;

import com.example.techwatch.keyword.Keyword;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ManualCsvJobMarketSource implements JobMarketSource {
    @Override
    public List<JobMarketSnapshot> load(Path source, List<Keyword> keywords) throws Exception {
        if (source == null || !Files.exists(source)) return List.of();
        Map<String, Keyword> byName = new HashMap<>();
        keywords.forEach(keyword -> byName.put(keyword.getNormalizedName(), keyword));
        List<JobMarketSnapshot> values = new ArrayList<>();
        List<String> lines = Files.readAllLines(source);
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber).trim();
            if (line.isBlank() || line.startsWith("#")) continue;
            List<String> columns = parse(line);
            if (columns.size() < 6) throw new IllegalArgumentException("求人CSV " + (lineNumber + 1) + "行目の列が不足しています");
            Keyword keyword = byName.get(columns.get(1).trim().toLowerCase(Locale.ROOT));
            if (keyword == null) continue;
            values.add(new JobMarketSnapshot(keyword.getId(), columns.get(2).trim().toUpperCase(Locale.ROOT),
                    columns.get(3).trim(), columns.get(4).trim(), Integer.parseInt(columns.get(5).trim()),
                    null, null, null, Instant.now(), LocalDate.parse(columns.get(0).trim())));
        }
        return values;
    }

    private List<String> parse(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char value = line.charAt(i);
            if (value == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                else quoted = !quoted;
            } else if (value == ',' && !quoted) { values.add(current.toString()); current.setLength(0); }
            else current.append(value);
        }
        values.add(current.toString());
        return values;
    }
}
