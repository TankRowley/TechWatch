package com.example.techwatch.display;

import com.example.techwatch.summarize.ArticleSummary;

public final class JapaneseSummaryFormatter {
    private JapaneseSummaryFormatter() { }

    public static String visibleSummary(ArticleSummary summary) {
        if (summary == null || summary.shortSummary().isBlank()) return "日本語要約はまだ生成されていません。";
        if (!containsJapanese(summary.shortSummary())) return "日本語要約はまだ生成されていません。AI要約を有効にすると日本語で表示されます。";
        return summary.shortSummary();
    }

    public static boolean containsJapanese(String value) {
        return value != null && value.codePoints().anyMatch(code ->
                (code >= 0x3040 && code <= 0x30ff) || (code >= 0x4e00 && code <= 0x9fff));
    }
}
