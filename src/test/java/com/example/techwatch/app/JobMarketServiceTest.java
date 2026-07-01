package com.example.techwatch.app;
import com.example.techwatch.db.*;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.market.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*; import java.time.LocalDate; import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class JobMarketServiceTest {
 @TempDir Path temp;
 @Test void missingCsvDoesNotCreateZeroDemand() throws Exception {
  Database db=new Database(temp.resolve("m.db")); db.initialize(); KeywordRepository kr=new KeywordRepository(db);
  Keyword java=kr.save(new Keyword("Java","Java","Core",5)); Path csv=temp.resolve("jobs.csv");
  Files.writeString(csv,"week_start,keyword,region,source,query,job_count\n");
  KeywordMarketStatsRepository stats=new KeywordMarketStatsRepository(db);
  var service=new JobMarketService(new ManualCsvJobMarketSource(),new JobMarketSnapshotRepository(db),stats,kr,new KeywordMarketEvaluator());
  assertTrue(service.refresh(csv,List.of(java),LocalDate.of(2026,6,29)).isEmpty());
  assertEquals(0,stats.findRecent(java.getId(),12).size());
 }
 @Test void oneKeywordDoesNotCreateZerosForOthers() throws Exception {
  Database db=new Database(temp.resolve("p.db")); db.initialize(); KeywordRepository kr=new KeywordRepository(db);
  Keyword java=kr.save(new Keyword("Java","Java","Core",5)); Keyword sql=kr.save(new Keyword("SQL","Data","Core",5));
  Path csv=temp.resolve("jobs2.csv"); Files.writeString(csv,"week_start,keyword,region,source,query,job_count\n2026-06-29,Java,US,manual,Java developer,1000\n");
  KeywordMarketStatsRepository stats=new KeywordMarketStatsRepository(db);
  var service=new JobMarketService(new ManualCsvJobMarketSource(),new JobMarketSnapshotRepository(db),stats,kr,new KeywordMarketEvaluator());
  var result=service.refresh(csv,List.of(java,sql),LocalDate.of(2026,6,29)); assertEquals(1,result.size()); assertTrue(result.containsKey(java.getId()));
 }
}
