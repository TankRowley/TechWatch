package com.example.techwatch;

import com.example.techwatch.app.WeeklyRunService;
import com.example.techwatch.app.CleanupService;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.RetentionConfigLoader;
import com.example.techwatch.db.Database;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            System.out.println("TechWatch - 技術情報週報ジェネレーター");
            System.out.println("Usage: java -jar techwatch.jar [run-weekly|cleanup|vacuum]");
            return;
        }
        try {
            String command = args.length == 0 ? "run-weekly" : args[0];
            if ("cleanup".equals(command) || "vacuum".equals(command)) {
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
            System.err.println("TechWatch failed: " + error.getMessage());
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
