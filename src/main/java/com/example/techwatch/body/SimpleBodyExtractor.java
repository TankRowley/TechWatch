package com.example.techwatch.body;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.function.Function;

public class SimpleBodyExtractor implements BodyExtractor {
    private final Function<String, Document> documentLoader;

    public SimpleBodyExtractor() {
        this(url -> {
            try {
                Connection connection = Jsoup.connect(url).userAgent("TechWatch/1.0 (+local research tool)")
                        .timeout(15_000).maxBodySize(2_000_000).followRedirects(true);
                return connection.get();
            } catch (Exception error) {
                throw new BodyLoadException(error);
            }
        });
    }

    public SimpleBodyExtractor(Function<String, Document> documentLoader) { this.documentLoader = documentLoader; }

    @Override
    public BodyExtractionResult extract(String url) {
        if (url == null || url.isBlank()) return BodyExtractionResult.skipped();
        try {
            Document document = documentLoader.apply(url);
            String rawHtml = document.outerHtml();
            document.select("script,style,nav,footer,header,aside,form,.advertisement,.ads,.related,.social-share").remove();
            Element content = first(document, "article", "main", "[role=main]", ".post-content", ".entry-content", ".article-body");
            String text = (content == null ? document.body() : content).text().replaceAll("\\s+", " ").trim();
            if (text.length() < 100) return BodyExtractionResult.failed("本文候補が短すぎます");
            BodyStatus status = text.length() >= 500 ? BodyStatus.SUCCESS : BodyStatus.PARTIAL;
            return new BodyExtractionResult(status, text, rawHtml, "");
        } catch (RuntimeException error) {
            Throwable cause = error instanceof BodyLoadException && error.getCause() != null ? error.getCause() : error;
            return BodyExtractionResult.failed(cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }

    private Element first(Document document, String... selectors) {
        for (String selector : selectors) {
            Element candidate = document.selectFirst(selector);
            if (candidate != null) return candidate;
        }
        return null;
    }

    private static final class BodyLoadException extends RuntimeException {
        private BodyLoadException(Throwable cause) { super(cause); }
    }
}
