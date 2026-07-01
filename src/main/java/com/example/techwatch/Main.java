package com.example.techwatch;

import com.example.techwatch.app.WeeklyRunService;
import com.example.techwatch.app.CleanupService;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.RetentionConfigLoader;
import com.example.techwatch.db.Database;
import com.example.techwatch.db.KeywordRepository;
import com.example.techwatch.db.KeywordWeeklyStatsRepository;
import com.example.techwatch.keyword.KeywordTrendEvaluator;
import java.time.LocalDate;
import java.time.ZoneId;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            System.out.println("てっくにゅーす - 技術情報週報ジェネレーター");
            System.out.println("Usage: java -jar techwatch.jar [run-weekly|backtest|cleanup|vacuum]");
            return;
        }
        try {
            String command = args.length == 0 ? "run-weekly" : args[0];
            if ("backtest".equals(command)) {
                AppPaths paths=AppPaths.detect(); paths.ensureDirectories();
                Database database=new Database(paths.database()); database.initialize();
                System.out.println(new com.example.techwatch.app.TrendBacktestService(new KeywordRepository(database),
                        new KeywordWeeklyStatsRepository(database),new KeywordTrendEvaluator())
                        .evaluate(LocalDate.now(ZoneId.of("Asia/Tokyo"))).summaryJapanese());
            } else if ("cleanup".equals(command) || "vacuum".equals(command)) {
                AppPaths paths = AppPaths.detect();
                CleanupService cleanup = new CleanupService(new Database(paths.database()), paths,
                        new RetentionConfigLoader().load(paths.retentionConfig()));
                System.out.println(cleanup.cleanup("vacuum".equals(command)).summaryJapanese());
            } else if ("run-weekly".equals(command)) {
                new WeeklyRunService().runWeekly();
            } else {
                throw new IllegalArgumentException("不明なコマンド: " + command);
            }
        } catch (Exception error) {
            System.err.println("てっくにゅーすの実行に失敗しました: " + error.getMessage());
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
