package com.example.techwatch.body;

public record BodyExtractionResult(BodyStatus status, String bodyText, String errorMessage) {
    public BodyExtractionResult {
        status = status == null ? BodyStatus.FAILED : status;
        bodyText = bodyText == null ? "" : bodyText;
        errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public static BodyExtractionResult skipped() { return new BodyExtractionResult(BodyStatus.SKIPPED, "", ""); }
    public static BodyExtractionResult failed(String message) { return new BodyExtractionResult(BodyStatus.FAILED, "", message); }
}
