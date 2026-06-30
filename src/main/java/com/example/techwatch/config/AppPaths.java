package com.example.techwatch.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AppPaths {
    private final Path home;

    public AppPaths(Path home) {
        this.home = home.toAbsolutePath().normalize();
    }

    public static AppPaths detect() {
        String configured = System.getProperty("techwatch.home");
        if (configured == null || configured.isBlank()) configured = System.getenv("TECHWATCH_HOME");
        if ((configured == null || configured.isBlank()) && isPackagedApplication()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                configured = Path.of(localAppData).resolve("TechWatch").toString();
            } else {
                configured = Path.of(System.getProperty("user.home"), ".techwatch").toString();
            }
        }
        if (configured == null || configured.isBlank()) configured = System.getProperty("user.dir");
        return new AppPaths(Path.of(configured));
    }

    private static boolean isPackagedApplication() {
        if (Boolean.getBoolean("techwatch.packaged")) return true;
        String appPath = System.getProperty("jpackage.app-path");
        return appPath != null && !appPath.isBlank();
    }

    public Path home() { return home; }
    public Path database() { return home.resolve("techwatch.db"); }
    public Path reportsDirectory() { return home.resolve("reports").resolve("weekly"); }
    public Path logsDirectory() { return home.resolve("logs"); }

    public Path sourceConfig() throws IOException { return ensureConfig("sources.yml"); }
    public Path keywordConfig() throws IOException { return ensureConfig("keywords.yml"); }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(home);
        Files.createDirectories(reportsDirectory());
        Files.createDirectories(logsDirectory());
    }

    private Path ensureConfig(String name) throws IOException {
        Path external = home.resolve("config").resolve(name);
        if (Files.exists(external)) return external;
        Path root = home.resolve(name);
        if (Files.exists(root)) return root;
        Files.createDirectories(external.getParent());
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path packagedConfig = Path.of(appPath).toAbsolutePath().getParent().resolve("config").resolve(name);
            if (Files.exists(packagedConfig)) {
                Files.copy(packagedConfig, external, StandardCopyOption.REPLACE_EXISTING);
                return external;
            }
        }
        try (InputStream in = AppPaths.class.getResourceAsStream("/defaults/" + name)) {
            if (in == null) throw new IOException("既定設定が見つかりません: " + name);
            Files.copy(in, external, StandardCopyOption.REPLACE_EXISTING);
        }
        return external;
    }
}
