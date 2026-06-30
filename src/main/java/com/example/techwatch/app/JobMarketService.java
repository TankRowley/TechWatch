package com.example.techwatch.app;

import com.example.techwatch.db.JobMarketSnapshotRepository;
import com.example.techwatch.db.KeywordMarketStatsRepository;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.market.JobMarketSnapshot;
import com.example.techwatch.market.JobMarketSource;
import com.example.techwatch.market.KeywordMarketEvaluator;
import com.example.techwatch.market.KeywordMarketStats;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class JobMarketService {
    private final JobMarketSource source;
    private final JobMarketSnapshotRepository snapshots;
    private final KeywordMarketStatsRepository stats;
    private final KeywordRepository keywords;
    private final KeywordMarketEvaluator evaluator;

    public JobMarketService(JobMarketSource source, JobMarketSnapshotRepository snapshots,
                            KeywordMarketStatsRepository stats, KeywordRepository keywords,
                            KeywordMarketEvaluator evaluator) {
        this.source = source; this.snapshots = snapshots; this.stats = stats;
        this.keywords = keywords; this.evaluator = evaluator;
    }

    public Map<Long, KeywordMarketStats> refresh(Path csv, List<Keyword> keywordList, LocalDate currentWeek)
            throws Exception {
        List<JobMarketSnapshot> imported = source.load(csv, keywordList);
        for (JobMarketSnapshot value : imported) snapshots.save(value);
        TreeSet<LocalDate> weeks = new TreeSet<>();
        imported.forEach(value -> weeks.add(value.weekStart()));
        weeks.add(currentWeek);
        for (LocalDate week : weeks) {
            Map<Long, List<JobMarketSnapshot>> byKeyword = snapshots.findForWeek(week).stream()
                    .collect(Collectors.groupingBy(JobMarketSnapshot::keywordId));
            for (Keyword keyword : keywordList) {
                List<JobMarketSnapshot> values = byKeyword.getOrDefault(keyword.getId(), List.of());
                int us = max(values, "US");
                int jp = max(values, "JP");
                KeywordMarketStats value = evaluator.evaluate(keyword, week, us, jp,
                        stats.findRecent(keyword.getId(), 12));
                stats.save(value);
                if (week.equals(currentWeek)) keywords.updateMarketScore(keyword.getId(), value.globalMarketScore());
            }
        }
        return stats.findLatestByKeyword();
    }

    private int max(List<JobMarketSnapshot> values, String region) {
        return values.stream().filter(value -> region.equalsIgnoreCase(value.region()))
                .mapToInt(JobMarketSnapshot::jobCount).max().orElse(0);
    }
}
