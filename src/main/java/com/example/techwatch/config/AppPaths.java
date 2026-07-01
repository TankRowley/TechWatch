package com.example.techwatch.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public final class AppPaths {
    private final Path home;

    public AppPaths(Path home) {
        this.home = home.toAbsolutePath().normalize();
    }

    public static AppPaths detect() {
        String configured = System.getProperty("techwatch.home");
        if (configured == null || configured.isBlank()) configured = System.getenv("TECHWATCH_HOME");
        if ((configured == null || configured.isBlank()) && isPackagedApplication()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                Path current = Path.of(localAppData).resolve("てっくにゅーす");
                Path legacy = Path.of(localAppData).resolve("TechWatch");
                configured = (!Files.exists(current) && Files.exists(legacy) ? legacy : current).toString();
            } else {
                configured = Path.of(System.getProperty("user.home"), ".techwatch").toString();
            }
        }
        if (configured == null || configured.isBlank()) configured = System.getProperty("user.dir");
        return new AppPaths(Path.of(configured));
    }

    private static boolean isPackagedApplication() {
        if (Boolean.getBoolean("techwatch.packaged")) return true;
        String appPath = System.getProperty("jpackage.app-path");
        return appPath != null && !appPath.isBlank();
    }

    public Path home() { return home; }
    public Path database() { return home.resolve("techwatch.db"); }
    public Path reportsDirectory() { return home.resolve("reports").resolve("weekly"); }
    public Path logsDirectory() { return home.resolve("logs"); }

    public Path sourceConfig() throws IOException { return ensureConfig("sources.yml"); }
    public Path keywordConfig() throws IOException { return ensureConfig("keywords.yml"); }
    public Path jobMarketCsv() throws IOException { return ensureConfig("job-market.csv"); }
    public Path retentionConfig() throws IOException { return ensureConfig("retention.yml"); }
    public Path mailConfig() throws IOException { return ensureConfig("email.yml"); }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(home);
        Files.createDirectories(reportsDirectory());
        Files.createDirectories(logsDirectory());
    }

    private Path ensureConfig(String name) throws IOException {
        Path external = home.resolve("config").resolve(name);
        if (Files.exists(external)) { mergeBundledDefaults(external, name); return external; }
        Path root = home.resolve(name);
        if (Files.exists(root)) { mergeBundledDefaults(root, name); return root; }
        Files.createDirectories(external.getParent());
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path packagedConfig = Path.of(appPath).toAbsolutePath().getParent().resolve("config").resolve(name);
            if (Files.exists(packagedConfig)) {
                Files.copy(packagedConfig, external, StandardCopyOption.REPLACE_EXISTING);
                return external;
            }
        }
        try (InputStream in = AppPaths.class.getResourceAsStream("/defaults/" + name)) {
            if (in == null) throw new IOException("既定設定が見つかりません: " + name);
            Files.copy(in, external, StandardCopyOption.REPLACE_EXISTING);
        }
        return external;
    }

    private void mergeBundledDefaults(Path path, String name) throws IOException {
        String section = switch (name) { case "sources.yml" -> "sources"; case "keywords.yml" -> "keywords"; default -> null; };
        String identity = "sources".equals(section) ? "url" : "name";
        if (section == null) return;
        try (InputStream defaults = AppPaths.class.getResourceAsStream("/defaults/" + name);
             InputStream existing = Files.newInputStream(path)) {
            if (defaults == null) return;
            Object existingRaw = new Yaml().load(existing); Object defaultRaw = new Yaml().load(defaults);
            if (!(existingRaw instanceof Map<?, ?> existingRoot) || !(defaultRaw instanceof Map<?, ?> defaultRoot)) return;
            if (!(existingRoot.get(section) instanceof List<?> oldItems)
                    || !(defaultRoot.get(section) instanceof List<?> defaultItems)) return;
            boolean legacySources = !"sources".equals(section) || hasLegacySourceSet(oldItems);
            List<Map<Object, Object>> merged = new ArrayList<>(); oldItems.forEach(item -> merged.add(copyMap(item)));
            boolean changed = false;
            for (Object defaultItem : defaultItems) {
                if (!(defaultItem instanceof Map<?, ?> defaultsMap)) continue;
                String key = text(defaultsMap.get(identity));
                Map<Object, Object> current = merged.stream()
                        .filter(item -> key.equalsIgnoreCase(text(item.get(identity)))).findFirst().orElse(null);
                if (current == null) {
                    if ("sources".equals(section) && legacySources) { merged.add(copyMap(defaultItem)); changed = true; }
                    continue;
                }
                for (String field : "sources".equals(section) ? List.of("category") : List.of("aliases")) {
                    if (!current.containsKey(field) && defaultsMap.containsKey(field)) {
                        current.put(field, defaultsMap.get(field)); changed = true;
                    }
                }
            }
            if (!changed) return;
            Path backup = path.resolveSibling(path.getFileName() + ".pre-1.4.bak");
            if (!Files.exists(backup)) Files.copy(path, backup);
            Map<Object, Object> output = new LinkedHashMap<>(); existingRoot.forEach(output::put);
            output.put(section, merged);
            DumperOptions options = new DumperOptions(); options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true); options.setIndent(2);
            Files.writeString(path, new Yaml(options).dump(output));
        }
    }

    private Map<Object, Object> copyMap(Object value) {
        Map<Object, Object> copy = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) map.forEach(copy::put);
        return copy;
    }
    private boolean hasLegacySourceSet(List<?> items) {
        List<String> urls = new ArrayList<>();
        for (Object item : items) if (item instanceof Map<?, ?> map) urls.add(text(map.get("url")));
        return urls.contains("https://blog.cloudflare.com/rss/")
                && urls.contains("https://netflixtechblog.com/feed")
                && urls.contains("https://www.databricks.com/feed");
    }
    private String text(Object value) { return value == null ? "" : value.toString().trim(); }
}
