package com.example.techwatch.score;

import com.example.techwatch.article.Article;
import com.example.techwatch.article.ArticleScore;
import com.example.techwatch.keyword.KeywordMatch;
import com.example.techwatch.source.Source;

import java.util.List;

public interface ArticleScorer {
    ArticleScore score(Article article, Source source, List<KeywordMatch> matches);
}
