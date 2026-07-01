package com.example.techwatch.app;

public record TrendBacktestResult(int evaluatedKeywords, int risingSignals, int risingValidated,
                                  int buzzSignals, int buzzValidated) {
    public double risingPrecision() { return risingSignals == 0 ? 0 : risingValidated * 100.0 / risingSignals; }
    public double buzzPrecision() { return buzzSignals == 0 ? 0 : buzzValidated * 100.0 / buzzSignals; }
    public String summaryJapanese() {
        if (risingSignals == 0 && buzzSignals == 0)
            return "バックテスト: 検証できる過去シグナルがまだありません。";
        return String.format(java.util.Locale.ROOT,
                "バックテスト: %dキーワード / Rising %d件中%d件継続 (%.1f%%) / Buzz %d件中%d件が短期終息 (%.1f%%)",
                evaluatedKeywords, risingSignals, risingValidated, risingPrecision(), buzzSignals, buzzValidated, buzzPrecision());
    }
}
