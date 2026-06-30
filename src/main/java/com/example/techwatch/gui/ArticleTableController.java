package com.example.techwatch.gui;

import com.example.techwatch.article.Article;
import com.example.techwatch.config.AppPaths;
import com.example.techwatch.db.ArticleRepository;
import com.example.techwatch.db.Database;
import com.example.techwatch.display.DisplayLabelMapper;
import com.example.techwatch.display.JapaneseSummaryFormatter;
import com.example.techwatch.summarize.ArticleSummary;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ArticleTableController {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();
    private final VBox root = new VBox(12);
    private final TableView<Article> table = new TableView<>();
    private final TextArea details = new TextArea();
    private final Button openButton = new Button("ブラウザーで記事を開く");
    private final Button saveButton = new Button("この記事を保存する");
    private Map<Long, ArticleSummary> summaries = Map.of();

    public ArticleTableController() {
        root.setPadding(new Insets(20));
        Label heading = new Label("記事一覧");
        heading.getStyleClass().add("section-title");
        Label guidance = new Label("評価点は、情報源・キーワード・学習中・固定・興味領域をもとに計算します。");
        guidance.getStyleClass().add("guidance");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("評価点", 75, a -> String.format("%.1f", a.getArticleScore())));
        table.getColumns().add(column("区分", 95, a -> labels.articleLabel(a.getImportanceLabel())));
        table.getColumns().add(column("記事タイトル", 430, Article::getTitle));
        table.getColumns().add(column("情報源", 150, Article::getSourceName));
        table.getColumns().add(column("公開日", 110, this::published));
        table.getColumns().add(column("保存", 70, a -> a.isSavedByUser() ? "保存中" : "-"));
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(10);
        details.setPromptText("記事を選ぶと、日本語要約と学習判断が表示されます");
        openButton.setDisable(true);
        openButton.setOnAction(event -> openSelected());
        saveButton.setDisable(true);
        saveButton.setOnAction(event -> toggleSaved());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, article) -> show(article));
        root.getChildren().addAll(heading, guidance, table, new Label("記事の判断メモ"), details,
                new HBox(10, openButton, saveButton));
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node view() { return root; }
    public void update(List<Article> articles, Map<Long, ArticleSummary> summaries) {
        this.summaries = summaries == null ? Map.of() : summaries;
        table.getItems().setAll(articles);
        show(table.getSelectionModel().getSelectedItem());
    }

    private TableColumn<Article, String> column(String title, double width, java.util.function.Function<Article, String> value) {
        TableColumn<Article, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private String published(Article article) {
        return article.getPublishedAt() == null ? "-" : DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("Asia/Tokyo")).format(article.getPublishedAt());
    }

    private void show(Article article) {
        openButton.setDisable(article == null);
        saveButton.setDisable(article == null);
        saveButton.setText(article != null && article.isSavedByUser() ? "保存を解除する" : "この記事を保存する");
        if (article == null) { details.clear(); return; }
        ArticleSummary summary = summaries.get(article.getId());
        StringBuilder text = new StringBuilder("概要:\n").append(JapaneseSummaryFormatter.visibleSummary(summary));
        if (summary != null) {
            if (!summary.technicalPoints().isEmpty()) {
                text.append("\n\n重要ポイント:\n");
                summary.technicalPoints().forEach(point -> text.append("・").append(point).append("\n"));
            }
            if (!summary.whyItMatters().isBlank()) text.append("\n自分に関係ある理由:\n").append(summary.whyItMatters());
            text.append("\n\n今読むべきか:\n").append(labels.learningPriority(summary.learningPriority()));
            if (!summary.prerequisites().isEmpty()) text.append("\n\n前提知識:\n").append(String.join(" / ", summary.prerequisites()));
        }
        text.append("\n\nリンク:\n").append(article.getUrl());
        details.setText(text.toString());
    }

    private void openSelected() {
        Article article = table.getSelectionModel().getSelectedItem();
        if (article == null) return;
        try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(article.getUrl())); }
        catch (Exception error) { details.appendText("\n\nURLを開けませんでした: " + error.getMessage()); }
    }

    private void toggleSaved() {
        Article article = table.getSelectionModel().getSelectedItem();
        if (article == null || article.getId() == null) return;
        boolean next = !article.isSavedByUser();
        try {
            Database database = new Database(AppPaths.detect().database());
            database.initialize();
            new ArticleRepository(database).setSavedByUser(article.getId(), next);
            article.setSavedByUser(next);
            table.refresh();
            show(article);
            details.appendText(next ? "\n\nこの記事はデータ整理から保護されます。" : "\n\n記事の保存指定を解除しました。");
        } catch (Exception error) {
            details.appendText("\n\n保存状態を変更できませんでした: " + error.getMessage());
        }
    }
}
