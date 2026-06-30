package com.example.techwatch.gui;

import com.example.techwatch.keyword.Keyword;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

public class KeywordTableController {
    private final VBox root = new VBox(12);
    private final TableView<Keyword> table = new TableView<>();

    public KeywordTableController() {
        root.setPadding(new Insets(20));
        Label heading = new Label("キーワード評価");
        heading.getStyleClass().add("section-title");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Keyword", 180, Keyword::getName));
        table.getColumns().add(column("Status", 100, Keyword::getStatus));
        table.getColumns().add(column("Trend", 80, k -> number(k.getTrendScore())));
        table.getColumns().add(column("Stability", 90, k -> number(k.getStabilityScore())));
        table.getColumns().add(column("Buzz Risk", 90, k -> number(k.getBuzzRiskScore())));
        table.getColumns().add(column("Recommendation", 230, this::recommendation));
        root.getChildren().addAll(heading, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node view() { return root; }
    public void update(List<Keyword> keywords) { table.getItems().setAll(keywords); }

    private TableColumn<Keyword, String> column(String title, double width, Function<Keyword, String> value) {
        TableColumn<Keyword, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private String recommendation(Keyword keyword) {
        return switch (keyword.getStatus()) {
            case "Core" -> "基礎として継続学習";
            case "Watch" -> "継続監視・次の学習候補";
            case "Buzz" -> "追跡するが固執しない";
            case "Decline" -> "優先度を下げる";
            default -> "必要時のみ確認";
        };
    }

    private String number(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.1f", value); }
}
