package com.example.techwatch.gui;

import com.example.techwatch.article.Article;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ArticleTableController {
    private final VBox root = new VBox(12);
    private final TableView<Article> table = new TableView<>();
    private final TextArea details = new TextArea();
    private final Button openButton = new Button("ブラウザーで記事を開く");

    public ArticleTableController() {
        root.setPadding(new Insets(20));
        Label heading = new Label("記事一覧");
        heading.getStyleClass().add("section-title");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Score", 80, a -> String.format("%.1f", a.getArticleScore())));
        table.getColumns().add(column("Label", 110, Article::getImportanceLabel));
        table.getColumns().add(column("Title", 420, Article::getTitle));
        table.getColumns().add(column("Source", 150, Article::getSourceName));
        table.getColumns().add(column("Published", 130, this::published));
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(6);
        details.setPromptText("記事を選ぶと概要が表示されます");
        openButton.setDisable(true);
        openButton.setOnAction(event -> openSelected());
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, article) -> show(article));
        root.getChildren().addAll(heading, table, new Label("概要"), details, openButton);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node view() { return root; }
    public void update(List<Article> articles) { table.getItems().setAll(articles); }

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
        details.setText(article == null ? "" : article.getSummaryOriginal() + "\n\n" + article.getUrl());
    }

    private void openSelected() {
        Article article = table.getSelectionModel().getSelectedItem();
        if (article == null) return;
        try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(article.getUrl())); }
        catch (Exception error) { details.appendText("\n\nURLを開けませんでした: " + error.getMessage()); }
    }
}
