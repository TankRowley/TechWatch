package com.example.techwatch;

import com.example.techwatch.app.WeeklyRunService;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
            System.out.println("TechWatch - 技術情報週報ジェネレーター");
            System.out.println("Usage: java -jar techwatch.jar [run-weekly]");
            return;
        }
        try {
            new WeeklyRunService().runWeekly();
        } catch (Exception error) {
            System.err.println("TechWatch failed: " + error.getMessage());
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
