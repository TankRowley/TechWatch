package com.example.techwatch.gui;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.keyword.Keyword;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;

public class DashboardController {
    private final VBox root = new VBox(18);
    private final Label conclusion = new Label("週報を読み込んでいます…");
    private final Label articleCount = metric("0", "評価記事");
    private final Label mustReadCount = metric("0", "Must Read");
    private final Label watchCount = metric("0", "Watch keywords");
    private final ListView<String> mustRead = new ListView<>();
    private final ListView<String> watchKeywords = new ListView<>();

    public DashboardController() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("dashboard");
        Label heading = new Label("今週のスナップショット");
        heading.getStyleClass().add("section-title");
        conclusion.setWrapText(true);
        conclusion.getStyleClass().add("conclusion");
        HBox metrics = new HBox(14, card(articleCount), card(mustReadCount), card(watchCount));
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        HBox lists = new HBox(18, section("Must Read", mustRead), section("追跡キーワード", watchKeywords));
        lists.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        mustRead.setPlaceholder(new Label("今週のMust Readはありません"));
        watchKeywords.setPlaceholder(new Label("キーワードの検出待ちです"));
        root.getChildren().addAll(heading, conclusion, metrics, new Separator(), lists);
    }

    public Node view() { return root; }

    public void update(WeeklyRunResult result) {
        articleCount.setText(Integer.toString(result.articles().size()));
        long must = result.articles().stream().filter(a -> "Must Read".equals(a.getImportanceLabel())).count();
        mustReadCount.setText(Long.toString(must));
        long watched = result.keywords().stream().filter(k -> "Watch".equalsIgnoreCase(k.getStatus())).count();
        watchCount.setText(Long.toString(watched));
        conclusion.setText(extractConclusion(result.reportMarkdown()));
        mustRead.getItems().setAll(result.articles().stream().filter(a -> "Must Read".equals(a.getImportanceLabel()))
                .limit(8).map(a -> String.format("%.1f  %s", a.getArticleScore(), a.getTitle())).toList());
        watchKeywords.getItems().setAll(result.keywords().stream()
                .filter(k -> "Watch".equalsIgnoreCase(k.getStatus()) || "Core".equalsIgnoreCase(k.getStatus()))
                .sorted(Comparator.comparingDouble(Keyword::getFinalScore).reversed()).limit(10)
                .map(k -> k.getName() + "  ·  " + k.getStatus()).toList());
    }

    private String extractConclusion(String markdown) {
        if (markdown == null || markdown.isBlank()) return "週報はまだありません。";
        String marker = "## 1. 今週の結論";
        int start = markdown.indexOf(marker);
        if (start < 0) return "最新の週報をReportsタブで確認できます。";
        start += marker.length();
        int end = markdown.indexOf("\n## ", start);
        return markdown.substring(start, end < 0 ? markdown.length() : end).trim();
    }

    private Label metric(String value, String label) {
        Label result = new Label(value);
        result.setUserData(label);
        result.getStyleClass().add("metric-value");
        return result;
    }

    private VBox card(Label value) {
        Label caption = new Label(value.getUserData().toString());
        caption.getStyleClass().add("metric-label");
        VBox box = new VBox(4, value, caption);
        box.getStyleClass().add("metric-card");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox section(String title, ListView<String> list) {
        Label label = new Label(title);
        label.getStyleClass().add("subsection-title");
        VBox box = new VBox(8, label, list);
        box.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }
}
