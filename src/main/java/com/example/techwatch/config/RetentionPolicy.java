package com.example.techwatch.config;

public record RetentionPolicy(int articleBodyDays, int rawHtmlDays, int executionLogDays,
                              int unselectedArticleDays, int articleMetadataDays,
                              int jobSnapshotDays, int htmlReportDays,
                              boolean keepMarkdownReports, boolean keepWeeklyKeywordStats,
                              boolean keepKeywordMarketStats) {
    public RetentionPolicy {
        articleBodyDays = nonNegative(articleBodyDays);
        rawHtmlDays = nonNegative(rawHtmlDays);
        executionLogDays = nonNegative(executionLogDays);
        unselectedArticleDays = nonNegative(unselectedArticleDays);
        articleMetadataDays = nonNegative(articleMetadataDays);
        jobSnapshotDays = nonNegative(jobSnapshotDays);
        htmlReportDays = nonNegative(htmlReportDays);
    }

    public static RetentionPolicy defaults() {
        return new RetentionPolicy(90, 30, 90, 365, 730, 730, 730, true, true, true);
    }

    private static int nonNegative(int value) { return Math.max(0, value); }
}
