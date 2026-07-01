package com.example.techwatch.fetch;

import com.example.techwatch.source.Source;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RssFeedFetcherTest {
    @Test
    void parsesRssWithoutNetwork() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss version="2.0"><channel><title>Test</title>
                  <item><title>Java and Docker</title><link>https://example.com/a</link>
                  <description>Technical article</description></item>
                </channel></rss>
                """;
        Source source = new Source(1L, "Test", "https://example.com/feed", "rss", 5, "ACTIVE");
        var articles = new RssFeedFetcher().parse(source,
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, articles.size());
        assertEquals("Java and Docker", articles.getFirst().getTitle());
        assertEquals(1L, articles.getFirst().getSourceId());
    }
}
