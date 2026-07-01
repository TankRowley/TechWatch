package com.example.techwatch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailConfigLoaderTest {
    @TempDir Path temp;

    @Test
    void savesAndLoadsNonSecretMailSettings() throws Exception {
        Path path = temp.resolve("config/email.yml");
        MailConfig expected = new MailConfig("reader@gmail.com");

        MailConfigLoader loader = new MailConfigLoader();
        loader.save(path, expected);
        MailConfig actual = loader.load(path);

        assertEquals(expected, actual);
        assertTrue(actual.hasRecipient());
    }
}
