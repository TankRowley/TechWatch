package com.example.techwatch.gui;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.app.WeeklyRunService;
import com.example.techwatch.config.AppPaths;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class TechWatchGuiApp extends Application {
    private final WeeklyRunService service = new WeeklyRunService();
    private final DashboardController dashboard = new DashboardController();
    private final ArticleTableController articles = new ArticleTableController();
    private final KeywordTableController keywords = new KeywordTableController();
    private final TextArea report = textArea();
    private final TextArea logs = textArea();
    private final TabPane tabs = new TabPane();
    private final Button runButton = new Button("週報を生成");
    private final Label status = new Label("準備中");

    @Override
    public void start(Stage stage) {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setTop(header());
        Tab dashboardTab = tab("Dashboard", dashboard.view());
        Tab articlesTab = tab("Articles", articles.view());
        Tab keywordsTab = tab("Keywords", keywords.view());
        Tab reportsTab = tab("Reports", report);
        Tab settingsTab = tab("Settings", settings());
        Tab logsTab = tab("Logs", logs);
        tabs.getTabs().addAll(dashboardTab, articlesTab, keywordsTab, reportsTab, settingsTab, logsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        shell.setCenter(tabs);
        Scene scene = new Scene(shell, 1180, 780);
        scene.getStylesheets().add(getClass().getResource("/techwatch.css").toExternalForm());
        stage.setTitle("TechWatch");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
        loadExisting();

        runButton.setOnAction(event -> runWeekly());
        Button reportButton = (Button) shell.lookup("#reportButton");
        if (reportButton != null) reportButton.setOnAction(event -> tabs.getSelectionModel().select(reportsTab));
    }

    public static void launchApp(String[] args) { launch(args); }

    private HBox header() {
        Label brand = new Label("TechWatch");
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("技術の波を、学ぶ順番に変える。");
        subtitle.getStyleClass().add("subtitle");
        VBox title = new VBox(1, brand, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button reportButton = new Button("最新レポート");
        reportButton.setId("reportButton");
        runButton.getStyleClass().add("primary-button");
        status.getStyleClass().add("status");
        HBox header = new HBox(14, title, spacer, status, reportButton, runButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.getStyleClass().add("top-bar");
        return header;
    }

    private void loadExisting() {
        Task<WeeklyRunResult> task = new Task<>() {
            @Override protected WeeklyRunResult call() throws Exception { return service.loadLatest(); }
        };
        task.setOnSucceeded(event -> { update(task.getValue()); status.setText("準備完了"); });
        task.setOnFailed(event -> { status.setText("読込エラー"); logs.setText(message(task.getException())); });
        start(task);
    }

    private void runWeekly() {
        runButton.setDisable(true);
        status.setText("収集中…");
        logs.clear();
        Task<WeeklyRunResult> task = new Task<>() {
            @Override protected WeeklyRunResult call() throws Exception {
                return service.runWeekly(line -> Platform.runLater(() -> logs.appendText(line + "\n")));
            }
        };
        task.setOnSucceeded(event -> {
            update(task.getValue());
            status.setText("週報を更新しました");
            runButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            logs.appendText("\nERROR: " + message(task.getException()));
            status.setText("生成に失敗しました");
            runButton.setDisable(false);
            tabs.getSelectionModel().select(tabs.getTabs().size() - 1);
        });
        start(task);
    }

    private void update(WeeklyRunResult result) {
        dashboard.update(result);
        articles.update(result.articles());
        keywords.update(result.keywords());
        report.setText(result.reportMarkdown());
        if (!result.logs().isEmpty()) logs.setText(String.join("\n", result.logs()));
    }

    private VBox settings() {
        AppPaths paths = AppPaths.detect();
        Label heading = new Label("設定");
        heading.getStyleClass().add("section-title");
        Label note = new Label("MVPでは設定ファイルを直接編集します。変更は次回の週報生成時に反映されます。");
        note.setWrapText(true);
        TextArea locations = textArea();
        locations.setText("TechWatch home\n" + paths.home() + "\n\nSources\nconfig/sources.yml または sources.yml"
                + "\n\nKeywords\nconfig/keywords.yml または keywords.yml\n\nAI要約\nOPENAI_API_KEY（任意）\nOPENAI_MODEL（任意、既定: gpt-5-mini）");
        VBox box = new VBox(14, heading, note, locations);
        box.setPadding(new Insets(24));
        VBox.setVgrow(locations, Priority.ALWAYS);
        return box;
    }

    private Tab tab(String title, javafx.scene.Node content) { return new Tab(title, content); }
    private static TextArea textArea() { TextArea area = new TextArea(); area.setEditable(false); area.setWrapText(true); return area; }
    private void start(Task<?> task) { Thread.ofVirtual().name("techwatch-worker").start(task); }
    private String message(Throwable error) { return error == null ? "不明なエラー" : error.getClass().getSimpleName() + ": " + error.getMessage(); }
}
