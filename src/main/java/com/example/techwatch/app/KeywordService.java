package com.example.techwatch.app;

import com.example.techwatch.db.KeywordMentionRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.KeywordStats;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.keyword.KeywordEvaluationResult;
import com.example.techwatch.keyword.KeywordEvaluator;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeywordService {
    private final KeywordRepository keywordRepository;
    private final KeywordMentionRepository mentionRepository;
    private final KeywordEvaluator evaluator;

    public KeywordService(KeywordRepository keywordRepository, KeywordMentionRepository mentionRepository,
                          KeywordEvaluator evaluator) {
        this.keywordRepository = keywordRepository;
        this.mentionRepository = mentionRepository;
        this.evaluator = evaluator;
    }

    public List<Keyword> evaluate(Instant weekStart, Instant weekEnd) throws Exception {
        Map<Long, KeywordStats> byId = new HashMap<>();
        for (KeywordStats stats : mentionRepository.findStats(weekStart, weekEnd)) byId.put(stats.keywordId(), stats);
        for (Keyword keyword : keywordRepository.findAll()) {
            KeywordStats stats = byId.getOrDefault(keyword.getId(), new KeywordStats(keyword.getId(), 0, 0, 0, 0, 0));
            KeywordEvaluationResult result = evaluator.evaluate(keyword, stats, weekEnd);
            keywordRepository.updateEvaluation(keyword.getId(), result);
        }
        return keywordRepository.findAll();
    }
}
