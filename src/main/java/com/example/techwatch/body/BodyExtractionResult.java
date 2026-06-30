package com.example.techwatch.body;

public record BodyExtractionResult(BodyStatus status, String bodyText, String rawHtml, String errorMessage) {
    public BodyExtractionResult {
        status = status == null ? BodyStatus.FAILED : status;
        bodyText = bodyText == null ? "" : bodyText;
        rawHtml = rawHtml == null ? "" : rawHtml;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public BodyExtractionResult(BodyStatus status, String bodyText, String errorMessage) {
        this(status, bodyText, "", errorMessage);
    }

    public static BodyExtractionResult skipped() { return new BodyExtractionResult(BodyStatus.SKIPPED, "", "", ""); }
    public static BodyExtractionResult failed(String message) { return new BodyExtractionResult(BodyStatus.FAILED, "", "", message); }
}
