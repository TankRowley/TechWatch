package com.example.techwatch.app;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.keyword.Keyword;

import java.util.List;

public class KeywordPreferenceService {
    private final AppPaths paths;

    public KeywordPreferenceService() { this(AppPaths.detect()); }
    public KeywordPreferenceService(AppPaths paths) { this.paths = paths; }

    public List<Keyword> setPinned(long keywordId, boolean pinned, String reason) throws Exception {
        KeywordRepository repository = repository();
        repository.updatePinned(keywordId, pinned, reason);
        return repository.findAll();
    }

    public List<Keyword> setLearning(long keywordId, boolean learning, String reason) throws Exception {
        KeywordRepository repository = repository();
        repository.updateLearning(keywordId, learning, reason);
        return repository.findAll();
    }

    private KeywordRepository repository() throws Exception {
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();
        return new KeywordRepository(database);
    }
}
