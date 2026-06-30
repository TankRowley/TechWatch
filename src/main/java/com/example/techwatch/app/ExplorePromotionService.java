package com.example.techwatch.app;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.DiscoveredKeywordRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.explore.DiscoveredKeyword;
import com.example.techwatch.keyword.Keyword;

public class ExplorePromotionService {
    public void promote(DiscoveredKeyword discovered, boolean learning, boolean pinned) throws Exception {
        Database database = new Database(AppPaths.detect().database());
        database.initialize();
        KeywordRepository keywords = new KeywordRepository(database);
        Keyword keyword = keywords.save(new Keyword(discovered.name(), discovered.category(), "Candidate", 3));
        if (learning) keywords.updateLearning(keyword.getId(), true, "探索から学習中へ昇格");
        if (pinned) keywords.updatePinned(keyword.getId(), true, "探索から固定監視へ昇格");
        new DiscoveredKeywordRepository(database).markPromoted(discovered.id());
    }

    public void ignore(DiscoveredKeyword discovered) throws Exception {
        Database database = new Database(AppPaths.detect().database());
        database.initialize();
        new DiscoveredKeywordRepository(database).updateJudgement(discovered.id(), "IGNORE");
    }
}
