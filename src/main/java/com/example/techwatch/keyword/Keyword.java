package com.example.techwatch.keyword;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public class Keyword {
    private Long id;
    private final String name;
    private final String normalizedName;
    private final String category;
    private String status;
    private final int weight;
    private double trendScore;
    private double stabilityScore;
    private double marketScore;
    private double learningValueScore;
    private double buzzRiskScore;
    private double finalScore;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
    private boolean pinned;
    private Instant pinnedAt;
    private String pinReason;
    private boolean learning;
    private Instant learningSince;
    private String learningReason;
    private String trendState;

    public Keyword(Long id, String name, String normalizedName, String category, String status, int weight,
                   double trendScore, double stabilityScore, double marketScore,
                   double learningValueScore, double buzzRiskScore, double finalScore,
                   Instant firstSeenAt, Instant lastSeenAt) {
        this(id, name, normalizedName, category, status, weight, trendScore, stabilityScore, marketScore,
                learningValueScore, buzzRiskScore, finalScore, firstSeenAt, lastSeenAt,
                false, null, "", false, null, "");
    }

    public Keyword(Long id, String name, String normalizedName, String category, String status, int weight,
                   double trendScore, double stabilityScore, double marketScore,
                   double learningValueScore, double buzzRiskScore, double finalScore,
                   Instant firstSeenAt, Instant lastSeenAt, boolean pinned, Instant pinnedAt,
                   String pinReason, boolean learning, Instant learningSince, String learningReason) {
        this(id, name, normalizedName, category, status, weight, trendScore, stabilityScore, marketScore,
                learningValueScore, buzzRiskScore, finalScore, firstSeenAt, lastSeenAt, pinned, pinnedAt,
                pinReason, learning, learningSince, learningReason, "Dormant");
    }

    public Keyword(Long id, String name, String normalizedName, String category, String status, int weight,
                   double trendScore, double stabilityScore, double marketScore,
                   double learningValueScore, double buzzRiskScore, double finalScore,
                   Instant firstSeenAt, Instant lastSeenAt, boolean pinned, Instant pinnedAt,
                   String pinReason, boolean learning, Instant learningSince, String learningReason,
                   String trendState) {
        this.id = id;
        this.name = Objects.requireNonNullElse(name, "").trim();
        this.normalizedName = normalizedName == null || normalizedName.isBlank()
                ? this.name.toLowerCase(Locale.ROOT) : normalizedName.toLowerCase(Locale.ROOT);
        this.category = Objects.requireNonNullElse(category, "Other");
        this.status = Objects.requireNonNullElse(status, "Candidate");
        this.weight = Math.max(1, weight);
        this.trendScore = trendScore;
        this.stabilityScore = stabilityScore;
        this.marketScore = marketScore;
        this.learningValueScore = learningValueScore;
        this.buzzRiskScore = buzzRiskScore;
        this.finalScore = finalScore;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
        this.pinned = pinned;
        this.pinnedAt = pinnedAt;
        this.pinReason = Objects.requireNonNullElse(pinReason, "");
        this.learning = learning;
        this.learningSince = learningSince;
        this.learningReason = Objects.requireNonNullElse(learningReason, "");
        this.trendState = Objects.requireNonNullElse(trendState, "Dormant");
    }

    public Keyword(String name, String category, String status, int weight) {
        this(null, name, null, category, status, weight, 0, 0, 0, 0, 0, 0, null, null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public String getNormalizedName() { return normalizedName; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public int getWeight() { return weight; }
    public double getTrendScore() { return trendScore; }
    public double getStabilityScore() { return stabilityScore; }
    public double getMarketScore() { return marketScore; }
    public double getLearningValueScore() { return learningValueScore; }
    public double getBuzzRiskScore() { return buzzRiskScore; }
    public double getFinalScore() { return finalScore; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public boolean isPinned() { return pinned; }
    public Instant getPinnedAt() { return pinnedAt; }
    public String getPinReason() { return pinReason; }
    public boolean isLearning() { return learning; }
    public Instant getLearningSince() { return learningSince; }
    public String getLearningReason() { return learningReason; }
    public String getTrendState() { return trendState; }

    public void setPinned(boolean pinned, Instant pinnedAt, String pinReason) {
        this.pinned = pinned;
        this.pinnedAt = pinnedAt;
        this.pinReason = Objects.requireNonNullElse(pinReason, "");
    }

    public void setLearning(boolean learning, Instant learningSince, String learningReason) {
        this.learning = learning;
        this.learningSince = learningSince;
        this.learningReason = Objects.requireNonNullElse(learningReason, "");
    }

    public void setTrendState(String trendState) { this.trendState = Objects.requireNonNullElse(trendState, "Dormant"); }

    public void applyEvaluation(KeywordEvaluationResult result) {
        this.trendScore = result.trendScore();
        this.stabilityScore = result.stabilityScore();
        this.learningValueScore = result.learningValueScore();
        this.buzzRiskScore = result.buzzRiskScore();
        this.finalScore = result.finalScore();
        this.status = result.status();
    }
}
