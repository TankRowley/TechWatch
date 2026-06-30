package com.example.techwatch.gui;

import com.example.techwatch.app.ExplorePromotionService;
import com.example.techwatch.display.DisplayLabelMapper;
import com.example.techwatch.explore.DiscoveredKeyword;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class ExploreController {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();
    private final ExplorePromotionService promotions = new ExplorePromotionService();
    private final VBox root = new VBox(12);
    private final TableView<DiscoveredKeyword> table = new TableView<>();
    private Runnable onChanged = () -> { };

    public ExploreController() {
        root.setPadding(new Insets(20));
        Label heading = new Label("探索 — まだ知らない技術"); heading.getStyleClass().add("section-title");
        Label note = new Label("記事とAI要約から登録外の技術語を見つけます。自動では学習中にせず、気になったものだけ昇格します。");
        note.setWrapText(true); note.getStyleClass().add("guidance");
        Button learning = new Button("学習中にする"); Button pin = new Button("固定する");
        Button ignore = new Button("無視する"); Button detail = new Button("詳細を見る");
        learning.setOnAction(event -> promote(true, false)); pin.setOnAction(event -> promote(false, true));
        ignore.setOnAction(event -> ignore()); detail.setOnAction(event -> detail());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("未知キーワード", 150, DiscoveredKeyword::name));
        table.getColumns().add(column("カテゴリ", 130, value -> labels.category(value.category())));
        table.getColumns().add(column("説明", 300, DiscoveredKeyword::description));
        table.getColumns().add(column("学習判断", 110, value -> labels.exploreJudgement(value.learningJudgement())));
        table.getColumns().add(column("前提知識", 180, value -> String.join(" / ", value.prerequisites())));
        table.getColumns().add(column("出現数", 60, value -> Integer.toString(value.mentionCount())));
        table.getColumns().add(column("最後に発見", 100, value -> date(value.lastSeenAt())));
        root.getChildren().addAll(heading, note, new HBox(8, learning, pin, ignore, detail), table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node view() { return root; }
    public void update(List<DiscoveredKeyword> values) { table.getItems().setAll(values); }
    public void setOnChanged(Runnable value) { onChanged = value == null ? () -> { } : value; }

    private void promote(boolean learning, boolean pinned) {
        DiscoveredKeyword value = selected(); if (value == null) return;
        try { promotions.promote(value, learning, pinned); onChanged.run(); }
        catch (Exception error) { error(error); }
    }
    private void ignore() {
        DiscoveredKeyword value = selected(); if (value == null) return;
        try { promotions.ignore(value); onChanged.run(); } catch (Exception error) { error(error); }
    }
    private void detail() {
        DiscoveredKeyword value = selected(); if (value == null) return;
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                value.description() + "\n\n学習判断: " + labels.exploreJudgement(value.learningJudgement())
                        + "\n前提知識: " + String.join(" / ", value.prerequisites()));
        alert.setTitle(value.name()); alert.setHeaderText(labels.category(value.category())); alert.showAndWait();
    }
    private DiscoveredKeyword selected() {
        DiscoveredKeyword value = table.getSelectionModel().getSelectedItem();
        if (value == null) new Alert(Alert.AlertType.INFORMATION, "操作する未知キーワードを選んでください。").showAndWait();
        return value;
    }
    private void error(Exception error) { new Alert(Alert.AlertType.ERROR, error.getMessage()).showAndWait(); }
    private String date(java.time.Instant value) {
        return value == null ? "-" : DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(value);
    }
    private TableColumn<DiscoveredKeyword, String> column(String title, double width,
                                                           Function<DiscoveredKeyword, String> value) {
        TableColumn<DiscoveredKeyword, String> column = new TableColumn<>(title); column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue()))); return column;
    }
}
