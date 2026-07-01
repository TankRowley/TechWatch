package com.example.techwatch.keyword;

import com.example.techwatch.article.Article;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class KeywordExtractor {
    public List<KeywordMatch> extract(Article article, List<Keyword> keywords) {
        return extract(article, "", keywords);
    }
    public List<KeywordMatch> extract(Article article, String bodyText, List<Keyword> keywords) {
        String title = plain(article.getTitle());
        String summary = plain(article.getSummaryOriginal());
        String body = plain(bodyText);
        List<KeywordMatch> matches = new ArrayList<>();
        for (Keyword keyword : keywords) {
            Set<String> locations = new LinkedHashSet<>();
            List<String> terms = new ArrayList<>(); terms.add(keyword.getName()); terms.addAll(keyword.getAliases());
            for (String term : terms) {
                Pattern pattern = termPattern(term);
                if (pattern.matcher(title).find()) locations.add("title");
                if (pattern.matcher(summary).find()) locations.add("summary");
                if (!body.isBlank() && pattern.matcher(body).find()) locations.add("body");
            }
            if (!locations.isEmpty()) matches.add(new KeywordMatch(keyword, locations));
        }
        return List.copyOf(matches);
    }

    private Pattern termPattern(String term) {
        String[] parts = term.trim().split("[\\s\\p{Pd}_]+");
        String joined = java.util.Arrays.stream(parts).filter(part -> !part.isBlank()).map(Pattern::quote)
                .collect(java.util.stream.Collectors.joining("[\\s\\p{Pd}_]*"));
        boolean plural = term.matches(".*[A-Za-z]$") && !term.endsWith("s")
                && !term.equals(term.toUpperCase(java.util.Locale.ROOT));
        return Pattern.compile("(?iu)(?<![A-Za-z0-9])" + joined
                + (plural ? "(?:s|es)?" : "") + "(?![A-Za-z0-9])");
    }

    private String plain(String input) { return input == null ? "" : Jsoup.parse(input).text(); }
}
