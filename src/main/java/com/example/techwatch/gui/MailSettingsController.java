package com.example.techwatch.gui;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.config.MailConfig;
import com.example.techwatch.config.MailConfigLoader;
import com.example.techwatch.mail.GmailComposeService;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MailSettingsController {
    private final AppPaths paths;
    private final Supplier<WeeklyRunResult> latestResult;
    private final Consumer<String> openUrl;
    private final MailConfigLoader loader = new MailConfigLoader();
    private final GmailComposeService compose = new GmailComposeService();
    private final TextField recipient = new TextField();
    private final Label status = new Label();
    private final VBox view;

    public MailSettingsController(AppPaths paths, Supplier<WeeklyRunResult> latestResult,
                                  Consumer<String> openUrl) {
        this.paths = paths;
        this.latestResult = latestResult;
        this.openUrl = openUrl;
        Label heading = new Label("Gmailで送る");
        heading.getStyleClass().add("subsection-title");
        Label help = new Label("送信先を保存すると、要約と記事リンクを入力済みのGmail作成画面を開けます。"
                + "Gmail側で内容を確認して「送信」を押してください。パスワード設定は不要です。");
        help.setWrapText(true);
        help.getStyleClass().add("guidance");
        recipient.setPromptText("送信先@gmail.com");
        recipient.setPrefColumnCount(32);
        Button save = new Button("宛先を保存");
        save.setOnAction(event -> save());
        Button open = new Button("Gmail作成画面を開く");
        open.getStyleClass().add("primary-button");
        open.setOnAction(event -> openGmail());
        view = new VBox(10, heading, help, new HBox(10, new Label("送信先"), recipient),
                new HBox(10, save, open, status));
        view.setPadding(new Insets(14));
        view.getStyleClass().add("metric-card");
        load();
    }

    public Node view() { return view; }

    public void openGmail() {
        try {
            MailConfig config = saveConfig();
            GmailComposeService.GmailDraft draft = compose.create(config.recipient(), latestResult.get());
            openUrl.accept(draft.composeUrl());
            status.setText("Gmailを開きました");
        } catch (Exception error) {
            status.setText(error.getMessage());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Gmailで送る");
            alert.setHeaderText("Gmail作成画面を開けませんでした");
            alert.setContentText(error.getMessage());
            alert.showAndWait();
        }
    }

    private void load() {
        try {
            recipient.setText(loader.load(paths.mailConfig()).recipient());
        } catch (Exception error) {
            status.setText("読込失敗: " + error.getMessage());
        }
    }

    private void save() {
        try {
            saveConfig();
            status.setText("宛先を保存しました");
        } catch (Exception error) {
            status.setText("保存失敗: " + error.getMessage());
        }
    }

    private MailConfig saveConfig() throws Exception {
        MailConfig config = new MailConfig(recipient.getText());
        if (!config.hasRecipient()) throw new IllegalArgumentException("正しい送信先メールアドレスを入力してください");
        loader.save(paths.mailConfig(), config);
        return config;
    }
}
