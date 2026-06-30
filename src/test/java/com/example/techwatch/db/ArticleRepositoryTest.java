package com.example.techwatch.db;

import com.example.techwatch.article.Article;
import com.example.techwatch.source.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleRepositoryTest {
    @TempDir Path temp;
    private ArticleRepository articles;
    private Source source;

    @BeforeEach
    void setUp() throws Exception {
        Database database = new Database(temp.resolve("test.db"));
        database.initialize();
        articles = new ArticleRepository(database);
        source = new SourceRepository(database).save(new Source("Test", "https://example.com/feed", "rss", 4));
    }

    @Test
    void savesArticleAndSkipsDuplicateUrl() throws Exception {
        Article first = Article.fetched(source.id(), source.name(), "Java news", "https://example.com/a", Instant.now(), "Docker");
        Article duplicate = Article.fetched(source.id(), source.name(), "Duplicate", "https://example.com/a", Instant.now(), "");

        assertTrue(articles.save(first).isPresent());
        assertTrue(articles.save(duplicate).isEmpty());
        assertEquals("Java news", articles.findByUrl("https://example.com/a").orElseThrow().getTitle());
    }
}
