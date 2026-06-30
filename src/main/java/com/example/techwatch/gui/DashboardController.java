package com.example.techwatch.gui;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.display.DisplayLabelMapper;
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
    private final DisplayLabelMapper labels = new DisplayLabelMapper();
    private final VBox root = new VBox(18);
    private final Label conclusion = new Label("週報を読み込んでいます…");
    private final Label articleCount = metric("0", "評価した記事");
    private final Label mustReadCount = metric("0", "必読記事");
    private final Label pinnedCount = metric("0", "固定キーワード");
    private final Label discoveredCount = metric("0", "未知キーワード");
    private final ListView<String> mustRead = new ListView<>();
    private final ListView<String> pinnedKeywords = new ListView<>();
    private final ListView<String> discoveredKeywords = new ListView<>();

    public DashboardController() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("dashboard");
        Label heading = new Label("今週の概要");
        heading.getStyleClass().add("section-title");
        conclusion.setWrapText(true);
        conclusion.getStyleClass().add("conclusion");
        HBox metrics = new HBox(14, card(articleCount), card(mustReadCount), card(pinnedCount), card(discoveredCount));
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        HBox lists = new HBox(18, section("必読記事", mustRead), section("固定キーワードの動き", pinnedKeywords),
                section("今週見つかった未知キーワード", discoveredKeywords));
        lists.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        mustRead.setPlaceholder(new Label("今週の必読記事はありません"));
        pinnedKeywords.setPlaceholder(new Label("キーワード画面から継続監視したい技術を固定できます"));
        discoveredKeywords.setPlaceholder(new Label("未知キーワードはまだありません"));
        root.getChildren().addAll(heading, conclusion, metrics, new Separator(), lists);
    }

    public Node view() { return root; }

    public void update(WeeklyRunResult result) {
        articleCount.setText(Integer.toString(result.articles().size()));
        long must = result.articles().stream().filter(a -> "Must Read".equals(a.getImportanceLabel())).count();
        mustReadCount.setText(Long.toString(must));
        long pinned = result.keywords().stream().filter(Keyword::isPinned).count();
        pinnedCount.setText(Long.toString(pinned));
        discoveredCount.setText(Integer.toString(result.discoveredKeywords().size()));
        conclusion.setText(extractConclusion(result.reportMarkdown()));
        mustRead.getItems().setAll(result.articles().stream().filter(a -> "Must Read".equals(a.getImportanceLabel()))
                .limit(8).map(a -> String.format("%.1f点  %s", a.getArticleScore(), a.getTitle())).toList());
        pinnedKeywords.getItems().setAll(result.keywords().stream().filter(Keyword::isPinned)
                .sorted(Comparator.comparingDouble(Keyword::getTrendScore).reversed())
                .map(k -> "📌 " + k.getName() + "  ·  " + labels.keywordStatus(k.getStatus())
                        + "  ·  今週" + number(k.getTrendScore()) + "件").toList());
        discoveredKeywords.getItems().setAll(result.discoveredKeywords().stream().limit(8)
                .map(value -> value.name() + "  ·  " + labels.exploreJudgement(value.learningJudgement())).toList());
    }

    private String extractConclusion(String markdown) {
        if (markdown == null || markdown.isBlank()) return "週報はまだありません。";
        String marker = "## 1. 今週の結論";
        int start = markdown.indexOf(marker);
        if (start < 0) return "最新の週報を「週報」タブで確認できます。";
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

    private String number(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.1f", value); }
}
