package com.example.techwatch.article;

import java.time.Instant;
import java.util.Objects;

public class Article {
    private Long id;
    private Long sourceId;
    private String sourceName;
    private String title;
    private String url;
    private Instant publishedAt;
    private Instant fetchedAt;
    private String summaryOriginal;
    private String bodyStatus;
    private double articleScore;
    private String importanceLabel;
    private boolean archived;
    private boolean savedByUser;
    private boolean cleanupProtected;

    public Article(Long id, Long sourceId, String sourceName, String title, String url,
                   Instant publishedAt, Instant fetchedAt, String summaryOriginal,
                   String bodyStatus, double articleScore, String importanceLabel) {
        this(id, sourceId, sourceName, title, url, publishedAt, fetchedAt, summaryOriginal,
                bodyStatus, articleScore, importanceLabel, false, false, false);
    }

    public Article(Long id, Long sourceId, String sourceName, String title, String url,
                   Instant publishedAt, Instant fetchedAt, String summaryOriginal,
                   String bodyStatus, double articleScore, String importanceLabel,
                   boolean archived, boolean savedByUser, boolean cleanupProtected) {
        this.id = id;
        this.sourceId = sourceId;
        this.sourceName = Objects.requireNonNullElse(sourceName, "Unknown source");
        this.title = Objects.requireNonNullElse(title, "(untitled)").trim();
        this.url = Objects.requireNonNullElse(url, "").trim();
        this.publishedAt = publishedAt;
        this.fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
        this.summaryOriginal = Objects.requireNonNullElse(summaryOriginal, "").trim();
        this.bodyStatus = Objects.requireNonNullElse(bodyStatus, "SKIPPED");
        this.articleScore = articleScore;
        this.importanceLabel = Objects.requireNonNullElse(importanceLabel, "UNRATED");
        this.archived = archived;
        this.savedByUser = savedByUser;
        this.cleanupProtected = cleanupProtected;
    }

    public static Article fetched(Long sourceId, String sourceName, String title, String url,
                                  Instant publishedAt, String summaryOriginal) {
        return new Article(null, sourceId, sourceName, title, url, publishedAt, Instant.now(),
                summaryOriginal, "SKIPPED", 0, "UNRATED");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getFetchedAt() { return fetchedAt; }
    public String getSummaryOriginal() { return summaryOriginal; }
    public String getBodyStatus() { return bodyStatus; }
    public void setBodyStatus(String bodyStatus) { this.bodyStatus = bodyStatus; }
    public double getArticleScore() { return articleScore; }
    public void setArticleScore(double articleScore) { this.articleScore = articleScore; }
    public String getImportanceLabel() { return importanceLabel; }
    public void setImportanceLabel(String importanceLabel) { this.importanceLabel = importanceLabel; }
    public boolean isArchived() { return archived; }
    public boolean isSavedByUser() { return savedByUser; }
    public void setSavedByUser(boolean savedByUser) { this.savedByUser = savedByUser; }
    public boolean isCleanupProtected() { return cleanupProtected; }
}
