package com.example.techwatch.app;

import com.example.techwatch.config.AppPaths;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.UserProfileRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.profile.UserProfile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class UserSetupService {
    private final AppPaths paths;

    public UserSetupService() { this(AppPaths.detect()); }
    public UserSetupService(AppPaths paths) { this.paths = paths; }

    public boolean isSetupComplete() throws Exception {
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();
        return new UserProfileRepository(database).find().isPresent();
    }

    public UserSetupSelection load() throws Exception {
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();
        UserProfile profile = new UserProfileRepository(database).find().orElse(new UserProfile(null, "", "", "beginner"));
        List<Keyword> keywords = new KeywordRepository(database).findAll();
        Set<String> learning = keywords.stream().filter(Keyword::isLearning).map(Keyword::getName)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<String> pinned = keywords.stream().filter(Keyword::isPinned).map(Keyword::getName)
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return new UserSetupSelection(profile.displayName(), profile.primaryGoal(), learning, pinned,
                new UserProfileRepository(database).findEnabledInterests());
    }

    public void save(UserSetupSelection selection) throws Exception {
        paths.ensureDirectories();
        Database database = new Database(paths.database());
        database.initialize();
        KeywordRepository keywords = new KeywordRepository(database);
        for (String name : selection.learningKeywords()) {
            Keyword keyword = ensureKeyword(keywords, name);
            keywords.updateLearning(keyword.getId(), true, "初回設定で学習中に指定");
        }
        for (String name : selection.pinnedKeywords()) {
            Keyword keyword = ensureKeyword(keywords, name);
            keywords.updatePinned(keyword.getId(), true, "初回設定で継続監視に指定");
        }
        // Reopening setup replaces the selected sets instead of accumulating stale choices.
        for (Keyword keyword : keywords.findAll()) {
            if (keyword.isLearning() && !containsIgnoreCase(selection.learningKeywords(), keyword.getName())) {
                keywords.updateLearning(keyword.getId(), false, "");
            }
            if (keyword.isPinned() && !containsIgnoreCase(selection.pinnedKeywords(), keyword.getName())) {
                keywords.updatePinned(keyword.getId(), false, "");
            }
        }
        UserProfileRepository profiles = new UserProfileRepository(database);
        profiles.save(new UserProfile(null, selection.displayName(), selection.primaryGoal(), "beginner"));
        profiles.saveInterests(selection.interestCategories());
    }

    public Set<String> interestCategories() throws Exception {
        Database database = new Database(paths.database());
        database.initialize();
        return new UserProfileRepository(database).findEnabledInterests();
    }

    private Keyword ensureKeyword(KeywordRepository repository, String name) throws Exception {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        try { return repository.findByNormalizedName(normalized); }
        catch (Exception ignored) { return repository.save(new Keyword(name.trim(), "Other", "Candidate", 3)); }
    }

    private boolean containsIgnoreCase(Set<String> values, String target) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(target));
    }
}
