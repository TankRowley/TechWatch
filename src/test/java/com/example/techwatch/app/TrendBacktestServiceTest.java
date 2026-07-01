package com.example.techwatch.app;
import com.example.techwatch.db.*; import com.example.techwatch.keyword.*; import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path; import java.time.LocalDate; import static org.junit.jupiter.api.Assertions.assertTrue;
class TrendBacktestServiceTest {
 @TempDir Path temp;
 @Test void validatesPersistentRising() throws Exception {
  Database db=new Database(temp.resolve("b.db")); db.initialize(); KeywordRepository kr=new KeywordRepository(db);
  long id=kr.save(new Keyword("Tool","Other","Watch",3)).getId(); KeywordWeeklyStatsRepository stats=new KeywordWeeklyStatsRepository(db);
  LocalDate start=LocalDate.of(2026,1,5); int[] mentions={1,1,1,1,1,1,1,1,5,2,2,0,0};
  for(int i=0;i<mentions.length;i++){int c=mentions[i];stats.save(new KeywordWeeklyStats(id,start.plusWeeks(i),c,c>0?2:0,c>0?1:0,0,0,5,20,2,2,"SUCCESS",c>0?.5:1));}
  var result=new TrendBacktestService(kr,stats,new KeywordTrendEvaluator()).evaluate(start.plusWeeks(12));
  assertTrue(result.risingSignals()>0); assertTrue(result.risingValidated()>0);
 }
}
