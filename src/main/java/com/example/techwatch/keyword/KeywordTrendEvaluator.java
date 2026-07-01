package com.example.techwatch.keyword;

import java.util.List;

public class KeywordTrendEvaluator {
    public String evaluate(List<KeywordWeeklyStats> history) {
        return assess(history).state();
    }

    public KeywordTrendAssessment assess(List<KeywordWeeklyStats> history) {
        if (history == null || history.isEmpty()) return empty();
        List<KeywordWeeklyStats> ordered = history.stream()
                .filter(value -> value != null)
                .sorted(java.util.Comparator.comparing(KeywordWeeklyStats::weekStart)).toList();
        if (ordered.isEmpty()) return empty();
        var currentWeek = ordered.get(ordered.size() - 1).weekStart();
        List<KeywordWeeklyStats> valid = ordered.stream().filter(KeywordWeeklyStats::isObserved).toList();
        int exposure = valid.stream().mapToInt(KeywordWeeklyStats::totalArticleCount).sum();
        if (valid.isEmpty()) return empty();

        double shortRate = ewma(valid, currentWeek, 4, 2);
        double mediumRate = ewma(valid, currentWeek, 13, 6);
        double longRate = ewma(valid, currentWeek, 52, 13);
        List<KeywordWeeklyStats> recent13 = window(valid, currentWeek, 13);
        List<KeywordWeeklyStats> recent8 = window(valid, currentWeek, 8);
        int mentions13 = recent13.stream().mapToInt(KeywordWeeklyStats::mentionCount).sum();
        double activeRate = recent13.isEmpty() ? 0 : recent13.stream()
                .filter(value -> value.mentionCount() > 0).count() / (double) recent13.size();
        double officialRatio = mentions13 <= 0 ? 0 : Math.min(1, recent13.stream()
                .mapToInt(KeywordWeeklyStats::officialSourceCount).sum() / (double) mentions13);
        double concentration = weightedConcentration(recent13);
        double momentumRatio = (shortRate + 0.25) / (mediumRate + 0.25);

        double volume = Math.min(35, 18 * Math.log1p(shortRate));
        double momentum = clamp(log2(momentumRatio) * 18, -15, 25);
        double diversity = mentions13 < 2 ? 0 : 15 * (1 - concentration);
        double quality = 10 * officialRatio;
        double persistence = 15 * activeRate;
        double activity = clamp(volume + Math.max(0, momentum) + diversity + quality + persistence, 0, 100);
        double stability = clamp(100 * (0.65 * activeRate
                + 0.35 * Math.min(1, recent13.size() / 13.0)), 0, 100);

        KeywordWeeklyStats current = ordered.get(ordered.size() - 1);
        boolean concentratedSpike = current.isObserved() && current.mentionCount() >= 3
                && current.mentionRate() > Math.max(1, mediumRate * 2)
                && current.sourceConcentration() >= 0.75;
        double buzz = concentratedSpike
                ? clamp(35 + current.sourceConcentration() * 45 + Math.max(0, log2(momentumRatio)) * 10, 0, 100)
                : momentumRatio > 1.5 && concentration >= 0.8 ? 35 : 0;

        double coverage = valid.stream().mapToDouble(KeywordWeeklyStats::collectionCoverage).average().orElse(0);
        double confidence = clamp((1 - Math.exp(-exposure / 80.0))
                * Math.min(1, valid.size() / 13.0) * coverage * 100, 0, 100);
        String state = current.isObserved()
                ? state(valid, recent8, shortRate, mediumRate, longRate, activeRate, confidence, concentration)
                : "Insufficient";
        return new KeywordTrendAssessment(state, round(activity), round(stability), round(buzz),
                round(confidence), round(shortRate), round(mediumRate), round(longRate));
    }

    private String state(List<KeywordWeeklyStats> valid, List<KeywordWeeklyStats> recent8,
                         double shortRate, double mediumRate, double longRate, double activeRate,
                         double confidence, double concentration) {
        int recentMentions = recent8.stream().mapToInt(KeywordWeeklyStats::mentionCount).sum();
        if (valid.size() < 4 || confidence < 15) return "Insufficient";
        if (recent8.size() >= 4 && recentMentions <= 1) return "Dormant";
        KeywordWeeklyStats current = valid.get(valid.size() - 1);
        List<KeywordWeeklyStats> prior = window(valid, current.weekStart(), 4).stream()
                .filter(value -> value.weekStart().isBefore(current.weekStart())).toList();
        double priorRate = prior.stream().mapToDouble(KeywordWeeklyStats::mentionRate).average().orElse(0);
        if (current.mentionRate() > priorRate * 1.35 + 0.25
                && (current.sourceConcentration() < 0.8 || current.officialSourceCount() > 0)) return "Rising";
        if (shortRate > mediumRate * 1.35 + 0.25 && (concentration < 0.8
                || recent8.stream().mapToInt(KeywordWeeklyStats::officialSourceCount).sum() > 0)) return "Rising";
        if (longRate > 0.5 && shortRate < longRate * 0.65) return "Cooling";
        if (activeRate >= 0.5) return "Stable";
        return shortRate > mediumRate * 1.15 ? "Rising" : "Cooling";
    }

    private List<KeywordWeeklyStats> window(List<KeywordWeeklyStats> values, java.time.LocalDate current, int weeks) {
        java.time.LocalDate first = current.minusWeeks(weeks - 1L);
        return values.stream().filter(value -> !value.weekStart().isBefore(first)).toList();
    }

    private double ewma(List<KeywordWeeklyStats> values, java.time.LocalDate current, int weeks, double halfLife) {
        double weighted = 0;
        double weights = 0;
        for (KeywordWeeklyStats value : window(values, current, weeks)) {
            long age = java.time.temporal.ChronoUnit.WEEKS.between(value.weekStart(), current);
            double weight = Math.exp(-Math.log(2) * age / halfLife) * value.collectionCoverage();
            weighted += value.mentionRate() * weight;
            weights += weight;
        }
        return weights <= 0 ? 0 : weighted / weights;
    }

    private double weightedConcentration(List<KeywordWeeklyStats> values) {
        int mentions = values.stream().mapToInt(KeywordWeeklyStats::mentionCount).sum();
        if (mentions <= 0) return 1;
        return values.stream().mapToDouble(value -> value.sourceConcentration() * value.mentionCount()).sum() / mentions;
    }

    private double log2(double value) { return Math.log(Math.max(0.0001, value)) / Math.log(2); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private double round(double value) { return Math.round(value * 10.0) / 10.0; }
    private KeywordTrendAssessment empty() {
        return new KeywordTrendAssessment("Insufficient", 0, 0, 0, 0, 0, 0, 0);
    }
}
