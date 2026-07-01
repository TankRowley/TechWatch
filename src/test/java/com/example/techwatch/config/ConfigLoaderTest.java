package com.example.techwatch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {
    @TempDir Path temp;

    @Test
    void loadsSourcesAndKeywords() throws Exception {
        Path sources = temp.resolve("sources.yml");
        Path keywords = temp.resolve("keywords.yml");
        Files.writeString(sources, "sources:\n  - { name: Test Blog, type: rss, url: 'https://example.com/feed', trustScore: 5, category: OSS }\n");
        Files.writeString(keywords, "keywords:\n  - { name: Java, category: Java, status: Core, weight: 5, aliases: [OpenJDK] }\n");

        var sourceList = new SourceConfigLoader().load(sources);
        var keywordList = new KeywordConfigLoader().load(keywords);

        assertEquals(1, sourceList.size());
        assertEquals(5, sourceList.getFirst().trustScore());
        assertEquals("OSS", sourceList.getFirst().category());
        assertEquals("java", keywordList.getFirst().getNormalizedName());
        assertEquals(List.of("OpenJDK"), keywordList.getFirst().getAliases());
    }

    @Test
    void loadsRetentionPolicyAndUsesSafeDefaults() throws Exception {
        Path retention = temp.resolve("retention.yml");
        Files.writeString(retention, """
                retention:
                  articleBodyDays: 45
                  executionLogDays: 14
                  keepMarkdownReports: true
                """);

        RetentionPolicy policy = new RetentionConfigLoader().load(retention);

        assertEquals(45, policy.articleBodyDays());
        assertEquals(14, policy.executionLogDays());
        assertEquals(730, policy.articleMetadataDays());
        assertTrue(policy.keepMarkdownReports());
    }

    @Test
    void packagedApplicationUsesUserDataDirectory() {
        System.setProperty("techwatch.packaged", "true");
        try {
            Path home = AppPaths.detect().home();
            assertTrue(home.endsWith("てっくにゅーす") || home.endsWith("TechWatch") || home.endsWith(".techwatch"));
            assertTrue(!home.equals(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()));
        } finally {
            System.clearProperty("techwatch.packaged");
        }
    }

    @Test void upgradesLegacyThreeSourceConfigWithoutDroppingExistingEntries() throws Exception {
        Path config = temp.resolve("config"); Files.createDirectories(config);
        Path sources = config.resolve("sources.yml");
        Files.writeString(sources, "sources:\n  - { name: Custom, type: rss, url: 'https://custom.example/feed', trustScore: 3 }\n"
                + "  - { name: Cloudflare Blog, type: rss, url: 'https://blog.cloudflare.com/rss/', trustScore: 5 }\n"
                + "  - { name: Netflix TechBlog, type: rss, url: 'https://netflixtechblog.com/feed', trustScore: 4 }\n"
                + "  - { name: Databricks Blog, type: rss, url: 'https://www.databricks.com/feed', trustScore: 5 }\n");

        Path upgraded = new AppPaths(temp).sourceConfig();
        var values = new SourceConfigLoader().load(upgraded);

        assertTrue(values.stream().anyMatch(value -> value.url().contains("custom.example")));
        assertEquals(21, values.size());
        assertTrue(Files.exists(config.resolve("sources.yml.pre-1.4.bak")));
    }
}
