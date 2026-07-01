package com.example.techwatch.app;

public record CleanupResult(int bodyTextsCleared, int rawHtmlCleared, int summariesDeleted,
                            int articlesDeleted, int jobSnapshotsDeleted, int logFilesDeleted,
                            int htmlReportsDeleted, int markdownReportsDeleted,
                            int weeklyKeywordStatsDeleted, int keywordMarketStatsDeleted,
                            long databaseBytesBefore, long databaseBytesAfter,
                            boolean vacuumed) {
    public int databaseRowsChanged() {
        return bodyTextsCleared + rawHtmlCleared + summariesDeleted + articlesDeleted + jobSnapshotsDeleted
                + weeklyKeywordStatsDeleted + keywordMarketStatsDeleted;
    }

    public String summaryJapanese() {
        return "整理完了: 本文 " + bodyTextsCleared + "件、HTML " + rawHtmlCleared
                + "件、AI要約 " + summariesDeleted + "件、記事 " + articlesDeleted
                + "件、求人詳細 " + jobSnapshotsDeleted + "件、ログ " + logFilesDeleted
                + "件、HTML週報 " + htmlReportsDeleted + "件、Markdown週報 " + markdownReportsDeleted
                + "件、週次統計 " + weeklyKeywordStatsDeleted + "件、市場統計 " + keywordMarketStatsDeleted
                + "件" + (vacuumed ? "（VACUUM実行済み）" : "");
    }
}
