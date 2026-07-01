package com.example.techwatch.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class MailConfigLoader {
    public MailConfig load(Path path) throws IOException {
        if (!Files.exists(path)) return MailConfig.defaults();
        try (InputStream in = Files.newInputStream(path)) {
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root) || !(root.get("email") instanceof Map<?, ?> values)) {
                return MailConfig.defaults();
            }
            return new MailConfig(text(values.get("recipient")));
        }
    }

    public void save(Path path, MailConfig config) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("recipient", config.recipient());
        Files.writeString(path, new Yaml().dump(Map.of("email", values)), StandardCharsets.UTF_8);
    }

    private String text(Object value) { return value == null ? "" : value.toString().trim(); }
}
