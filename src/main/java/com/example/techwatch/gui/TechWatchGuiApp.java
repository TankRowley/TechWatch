package com.example.techwatch.gui;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.app.WeeklyRunService;
import com.example.techwatch.app.UserSetupService;
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
    private final ExploreController explore = new ExploreController();
    private final JobMarketController jobMarket = new JobMarketController();
    private final RetentionSettingsController retention = new RetentionSettingsController();
    private final TextArea report = textArea();
    private final TextArea logs = textArea();
    private final TabPane tabs = new TabPane();
    private final Button runButton = new Button("週報を生成");
    private final Label status = new Label("準備中");
    private WeeklyRunResult currentResult;

    @Override
    public void start(Stage stage) {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setTop(header());
        Tab dashboardTab = tab("概要", dashboard.view());
        Tab articlesTab = tab("記事", articles.view());
        Tab keywordsTab = tab("キーワード", keywords.view());
        Tab exploreTab = tab("探索", explore.view());
        Tab marketTab = tab("求人市場", jobMarket.view());
        Tab reportsTab = tab("週報", report);
        Tab settingsTab = tab("設定", settings(stage));
        Tab logsTab = tab("ログ", logs);
        tabs.getTabs().addAll(dashboardTab, articlesTab, keywordsTab, exploreTab, marketTab,
                reportsTab, settingsTab, logsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        shell.setCenter(tabs);
        Scene scene = new Scene(shell, 1180, 780);
        scene.getStylesheets().add(getClass().getResource("/techwatch.css").toExternalForm());
        stage.setTitle("TechWatch");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
        keywords.setOnKeywordsChanged(this::keywordsChanged);
        explore.setOnChanged(this::loadExisting);
        Platform.runLater(() -> initializeForUser(stage));

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
        Button reportButton = new Button("最新週報を開く");
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
        currentResult = result;
        dashboard.update(result);
        articles.update(result.articles(), result.summaries());
        keywords.update(result.keywords(), result.marketStats());
        explore.update(result.discoveredKeywords());
        jobMarket.update(result.keywords(), result.marketStats());
        report.setText(result.reportMarkdown());
        if (!result.logs().isEmpty()) logs.setText(String.join("\n", result.logs()));
    }

    private VBox settings(Stage stage) {
        AppPaths paths = AppPaths.detect();
        Label heading = new Label("設定");
        heading.getStyleClass().add("section-title");
        Label note = new Label("学習中・固定・興味領域は初期設定画面からいつでも変更できます。情報源やキーワード候補は設定ファイルでも編集できます。");
        note.setWrapText(true);
        Button setup = new Button("学習設定を開く");
        setup.getStyleClass().add("primary-button");
        setup.setOnAction(event -> {
            if (new FirstRunSetupDialog().show(stage, false)) loadExisting();
        });
        TextArea locations = textArea();
        locations.setText("データ保存先\n" + paths.home() + "\n\n情報源設定\nconfig/sources.yml または sources.yml"
                + "\n\nキーワード候補\nconfig/keywords.yml または keywords.yml\n\n日本語AI要約"
                + "\n\n求人市場CSV\nconfig/job-market.csv"
                + "\n\nデータ保持設定\nconfig/retention.yml"
                + "\nOPENAI_API_KEY（OpenAI利用時）\nOPENAI_MODEL（LM StudioではロードしたモデルID）"
                + "\nOPENAI_BASE_URL（LM Studio例: http://localhost:1234/v1）");
        VBox box = new VBox(14, heading, note, setup, retention.view(), locations);
        box.setPadding(new Insets(24));
        VBox.setVgrow(locations, Priority.ALWAYS);
        return box;
    }

    private void initializeForUser(Stage stage) {
        try {
            if (!new UserSetupService().isSetupComplete()) new FirstRunSetupDialog().show(stage, true);
        } catch (Exception error) {
            logs.setText("初期設定の確認に失敗しました: " + message(error));
        }
        loadExisting();
    }

    private void keywordsChanged(java.util.List<com.example.techwatch.keyword.Keyword> changed) {
        if (currentResult == null) return;
        currentResult = new WeeklyRunResult(currentResult.reportPath(), currentResult.reportMarkdown(),
                currentResult.articles(), changed, currentResult.summaries(), currentResult.logs(), currentResult.stats(),
                currentResult.discoveredKeywords(), currentResult.marketStats());
        dashboard.update(currentResult);
    }

    private Tab tab(String title, javafx.scene.Node content) { return new Tab(title, content); }
    private static TextArea textArea() { TextArea area = new TextArea(); area.setEditable(false); area.setWrapText(true); return area; }
    private void start(Task<?> task) { Thread.ofVirtual().name("techwatch-worker").start(task); }
    private String message(Throwable error) { return error == null ? "不明なエラー" : error.getClass().getSimpleName() + ": " + error.getMessage(); }
}
