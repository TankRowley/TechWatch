package com.example.techwatch.config;

public record MailConfig(String recipient) {
    public MailConfig {
        recipient = clean(recipient);
    }

    public static MailConfig defaults() {
        return new MailConfig("");
    }

    public boolean hasRecipient() {
        return recipient.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
