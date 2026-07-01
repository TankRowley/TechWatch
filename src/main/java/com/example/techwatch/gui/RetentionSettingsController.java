package com.example.techwatch.gui;

import com.example.techwatch.app.CleanupResult;
import com.example.techwatch.app.CleanupService;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.RetentionConfigLoader;
import com.example.techwatch.config.RetentionPolicy;
import com.example.techwatch.db.Database;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;

public class RetentionSettingsController {
    private final AppPaths paths = AppPaths.detect();
    private final Label policyText = new Label();
    private final Label status = new Label("DB容量を確認できます。");
    private final Button cleanupButton = new Button("今すぐ整理する");
    private final Button sizeButton = new Button("DB容量を確認する");
    private final Button vacuumButton = new Button("DBを圧縮する（VACUUM）");

    public VBox view() {
        Label heading = new Label("データ保存期間");
        heading.getStyleClass().add("section-title");
        policyText.setWrapText(true);
        status.setWrapText(true);
        cleanupButton.getStyleClass().add("primary-button");
        cleanupButton.setOnAction(event -> cleanup(false));
        sizeButton.setOnAction(event -> showSize());
        vacuumButton.setOnAction(event -> cleanup(true));
        reloadPolicy();
        VBox box = new VBox(10, heading, policyText,
                new HBox(10, cleanupButton, sizeButton, vacuumButton), status);
        box.setPadding(new Insets(10, 0, 0, 0));
        return box;
    }

    private void reloadPolicy() {
        try {
            RetentionPolicy policy = new RetentionConfigLoader().load(paths.retentionConfig());
            policyText.setText("本文: " + policy.articleBodyDays() + "日 / raw HTML: " + policy.rawHtmlDays()
                    + "日 / 実行ログ: " + policy.executionLogDays() + "日\n"
                    + "未採用記事: " + policy.unselectedArticleDays() + "日 / 記事メタデータ・AI要約: "
                    + policy.articleMetadataDays() + "日 / 求人詳細: " + policy.jobSnapshotDays() + "日\n"
                    + "Markdown週報: " + retention(policy.keepMarkdownReports(), policy.htmlReportDays())
                    + " / 週次キーワード統計: "
                    + retention(policy.keepWeeklyKeywordStats(), policy.articleMetadataDays())
                    + " / 市場統計: " + retention(policy.keepKeywordMarketStats(), policy.jobSnapshotDays()));
        } catch (Exception error) {
            policyText.setText("保持設定を読み込めません: " + error.getMessage());
        }
    }

    private void cleanup(boolean vacuum) {
        setBusy(true, vacuum ? "整理とDB圧縮を実行中…" : "古いデータを整理中…");
        Thread.ofVirtual().name("techwatch-cleanup").start(() -> {
            try {
                Database database = new Database(paths.database());
                RetentionPolicy policy = new RetentionConfigLoader().load(paths.retentionConfig());
                CleanupResult result = new CleanupService(database, paths, policy).cleanup(vacuum);
                Platform.runLater(() -> setBusy(false, result.summaryJapanese() + "\nDB容量: "
                        + size(result.databaseBytesBefore()) + " → " + size(result.databaseBytesAfter())));
            } catch (Exception error) {
                Platform.runLater(() -> setBusy(false, "整理に失敗しました: " + error.getMessage()));
            }
        });
    }

    private void showSize() {
        try {
            Database database = new Database(paths.database());
            RetentionPolicy policy = new RetentionConfigLoader().load(paths.retentionConfig());
            long bytes = new CleanupService(database, paths, policy).databaseSize();
            status.setText("現在のDB容量: " + size(bytes) + "\n保存先: " + paths.database());
        } catch (Exception error) {
            status.setText("DB容量を確認できません: " + error.getMessage());
        }
    }

    private void setBusy(boolean busy, String message) {
        cleanupButton.setDisable(busy);
        sizeButton.setDisable(busy);
        vacuumButton.setDisable(busy);
        status.setText(message);
    }

    private String size(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.JAPAN, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.JAPAN, "%.1f MB", bytes / 1024.0 / 1024.0);
    }

    private String retention(boolean keep, int days) { return keep ? "永続保存" : days + "日"; }
}
