package com.example.techwatch.net;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlSafetyPolicyTest {
    private final UrlSafetyPolicy policy = new UrlSafetyPolicy();

    @Test
    void rejectsLocalAndPrivateAddresses() {
        assertThrows(Exception.class, () -> policy.validate(URI.create("http://127.0.0.1/admin")));
        assertThrows(Exception.class, () -> policy.validate(URI.create("http://[::1]/admin")));
        assertThrows(Exception.class, () -> policy.validate(URI.create("http://10.0.0.1/metadata")));
        assertThrows(Exception.class, () -> policy.validate(URI.create("http://169.254.169.254/latest")));
        assertThrows(Exception.class, () -> policy.validate(URI.create("file:///etc/passwd")));
    }

    @Test
    void acceptsPublicHttpAddresses() {
        assertDoesNotThrow(() -> policy.validate(URI.create("https://1.1.1.1/feed")));
        assertDoesNotThrow(() -> policy.validate(URI.create("https://8.8.8.8/feed")));
    }
}
