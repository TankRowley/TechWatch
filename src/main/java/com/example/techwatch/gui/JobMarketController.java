package com.example.techwatch.gui;

import com.example.techwatch.display.DisplayLabelMapper;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.market.KeywordMarketStats;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JobMarketController {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();
    private final VBox root = new VBox(12);
    private final Label summary = new Label();
    private final TableView<Row> table = new TableView<>();

    public JobMarketController() {
        root.setPadding(new Insets(20));
        Label heading = new Label("求人市場シグナル"); heading.getStyleClass().add("section-title");
        Label note = new Label("求人件数は検索語やデータ元で変わります。絶対値ではなく、記事傾向との比較材料として使います。");
        note.setWrapText(true); note.getStyleClass().add("guidance");
        summary.setWrapText(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("キーワード", 150, Row::name));
        table.getColumns().add(column("状態", 90, row -> labels.keywordStatus(row.keyword().getStatus())));
        table.getColumns().add(column("最近の動き", 95, row -> labels.trendState(row.keyword().getTrendState())));
        table.getColumns().add(column("米国求人数", 90, row -> Integer.toString(row.market().usJobCount())));
        table.getColumns().add(column("日本求人数", 90, row -> Integer.toString(row.market().jpJobCount())));
        table.getColumns().add(column("市場評価", 110, row -> labels.marketLabel(row.market().marketLabel())));
        table.getColumns().add(column("学習リターン", 100, row -> String.format("%.1f", row.learningRoi())));
        table.getColumns().add(column("判断", 260, this::reason));
        table.setOnMouseClicked(event -> { if (event.getClickCount() == 2) detail(); });
        root.getChildren().addAll(heading, note, summary, table); VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node view() { return root; }
    public void update(List<Keyword> keywords, Map<Long, KeywordMarketStats> market) {
        List<Row> rows = keywords.stream().filter(keyword -> market.containsKey(keyword.getId()))
                .map(keyword -> new Row(keyword, market.get(keyword.getId()), roi(keyword, market.get(keyword.getId()))))
                .sorted(Comparator.comparingDouble(Row::learningRoi).reversed()).toList();
        table.getItems().setAll(rows);
        long us = rows.stream().filter(row -> "US Leading".equals(row.market().marketLabel())).count();
        long jp = rows.stream().filter(row -> "JP Strong".equals(row.market().marketLabel())).count();
        long buzz = rows.stream().filter(row -> "Buzz Only".equals(row.market().marketLabel())).count();
        summary.setText("米国先行: " + us + "件　国内需要あり: " + jp + "件　話題先行: " + buzz + "件");
    }
    private void detail() {
        Row row = table.getSelectionModel().getSelectedItem();
        if (row != null) new KeywordDetailDialog().show(table.getScene().getWindow(), row.keyword(), row.market());
    }
    private double roi(Keyword keyword, KeywordMarketStats market) {
        return Math.min(100, market.globalMarketScore() + (keyword.isLearning() ? 10 : 0)
                + (keyword.isPinned() ? 6 : 0) + ("Core".equals(keyword.getStatus()) ? 8 : 0)
                - keyword.getBuzzRiskScore());
    }
    private String reason(Row row) {
        return switch (row.market().marketLabel()) {
            case "Hot" -> "記事と求人の両方が強く、学習候補です";
            case "Stable Demand" -> "地味でも実務需要があり、継続価値があります";
            case "US Leading" -> "米国先行。国内動向を見ながら監視します";
            case "JP Strong" -> "国内求人との接続が強い技術です";
            case "Buzz Only" -> "話題先行。基礎より優先しすぎません";
            case "Declining Demand" -> "需要低下。優先度を見直します";
            default -> "データを蓄積して傾向を確認します";
        };
    }
    private TableColumn<Row, String> column(String title, double width, Function<Row, String> value) {
        TableColumn<Row, String> column = new TableColumn<>(title); column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue()))); return column;
    }
    private record Row(Keyword keyword, KeywordMarketStats market, double learningRoi) {
        String name() { return keyword.getName(); }
    }
}
