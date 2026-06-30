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
        String title = plain(article.getTitle());
        String summary = plain(article.getSummaryOriginal());
        List<KeywordMatch> matches = new ArrayList<>();
        for (Keyword keyword : keywords) {
            Pattern pattern = Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(keyword.getName())
                    + "(?![\\p{L}\\p{N}])");
            Set<String> locations = new LinkedHashSet<>();
            if (pattern.matcher(title).find()) locations.add("title");
            if (pattern.matcher(summary).find()) locations.add("summary");
            if (!locations.isEmpty()) matches.add(new KeywordMatch(keyword, locations));
        }
        return List.copyOf(matches);
    }

    private String plain(String input) { return input == null ? "" : Jsoup.parse(input).text(); }
}
