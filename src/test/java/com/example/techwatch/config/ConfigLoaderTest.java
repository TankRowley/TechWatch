package com.example.techwatch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {
    @TempDir Path temp;

    @Test
    void loadsSourcesAndKeywords() throws Exception {
        Path sources = temp.resolve("sources.yml");
        Path keywords = temp.resolve("keywords.yml");
        Files.writeString(sources, "sources:\n  - { name: Test Blog, type: rss, url: 'https://example.com/feed', trustScore: 5 }\n");
        Files.writeString(keywords, "keywords:\n  - { name: Java, category: Java, status: Core, weight: 5 }\n");

        var sourceList = new SourceConfigLoader().load(sources);
        var keywordList = new KeywordConfigLoader().load(keywords);

        assertEquals(1, sourceList.size());
        assertEquals(5, sourceList.getFirst().trustScore());
        assertEquals("java", keywordList.getFirst().getNormalizedName());
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
        assertEquals(365, policy.articleMetadataDays());
        assertTrue(policy.keepMarkdownReports());
    }

    @Test
    void packagedApplicationUsesUserDataDirectory() {
        System.setProperty("techwatch.packaged", "true");
        try {
            Path home = AppPaths.detect().home();
            assertTrue(home.endsWith("TechWatch") || home.endsWith(".techwatch"));
            assertTrue(!home.equals(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()));
        } finally {
            System.clearProperty("techwatch.packaged");
        }
    }
}
