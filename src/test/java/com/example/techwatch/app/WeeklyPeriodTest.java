package com.example.techwatch.app;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
class WeeklyPeriodTest {
 @Test void previousCompletedWeek(){
  var expected=new WeeklyPeriod(LocalDate.of(2026,6,22),LocalDate.of(2026,6,28));
  assertEquals(expected,WeeklyPeriod.previousCompleted(LocalDate.of(2026,7,1)));
  assertEquals(expected,WeeklyPeriod.previousCompleted(LocalDate.of(2026,6,29)));
 }
}
