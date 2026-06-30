package com.example.techwatch.market;

import com.example.techwatch.keyword.Keyword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManualCsvJobMarketSourceTest {
    @TempDir Path temp;

    @Test void readsUsAndJapaneseManualCountsWithoutScraping() throws Exception {
        Path csv = temp.resolve("jobs.csv");
        Files.writeString(csv, """
                week_start,keyword,region,source,query,job_count
                2026-06-29,Java,US,manual,Java developer,12000
                2026-06-29,Java,JP,manual,Java エンジニア,8500
                """);
        Keyword java = new Keyword(1L, "Java", "java", "Backend", "Core", 5,
                0, 0, 0, 0, 0, 0, null, null);

        List<JobMarketSnapshot> values = new ManualCsvJobMarketSource().load(csv, List.of(java));

        assertEquals(2, values.size());
        assertEquals(12000, values.get(0).jobCount());
        assertEquals("JP", values.get(1).region());
    }
}
