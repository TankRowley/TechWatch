package com.example.techwatch.fetch;

import com.example.techwatch.article.Article;
import com.example.techwatch.source.Source;
import com.example.techwatch.net.SafeHttpClient;
import com.example.techwatch.net.UrlSafetyPolicy;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RssFeedFetcher implements FeedFetcher {
    private final SafeHttpClient httpClient;

    public RssFeedFetcher() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    public RssFeedFetcher(HttpClient httpClient) {
        this.httpClient = new SafeHttpClient(httpClient, new UrlSafetyPolicy(), 5_000_000, 5);
    }

    @Override
    public FeedFetchResult fetch(Source source) {
        try {
            SafeHttpClient.Response response = httpClient.get(URI.create(source.url()),
                    "application/rss+xml, application/atom+xml, application/xml, text/xml, */*",
                    Duration.ofSeconds(20));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return FeedFetchResult.failure("HTTP " + response.statusCode());
            }
            return FeedFetchResult.success(parse(source, new ByteArrayInputStream(response.body())));
        } catch (Exception error) {
            return FeedFetchResult.failure(error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    public List<Article> parse(Source source, InputStream inputStream) throws Exception {
        SyndFeed feed;
        try (XmlReader reader = new XmlReader(inputStream)) {
            feed = new SyndFeedInput().build(reader);
        }
        List<Article> articles = new ArrayList<>();
        for (SyndEntry entry : feed.getEntries()) {
            String url = entry.getLink();
            if ((url == null || url.isBlank()) && !entry.getLinks().isEmpty()) url = entry.getLinks().getFirst().getHref();
            if (url == null || url.isBlank()) continue;
            Instant published = entry.getPublishedDate() != null ? entry.getPublishedDate().toInstant()
                    : entry.getUpdatedDate() != null ? entry.getUpdatedDate().toInstant() : null;
            String summary = entry.getDescription() == null ? "" : entry.getDescription().getValue();
            articles.add(Article.fetched(source.id(), source.name(), entry.getTitle(), url, published, summary));
        }
        return articles;
    }
}
