package com.example.techwatch.gui;

import com.example.techwatch.app.KeywordPreferenceService;
import com.example.techwatch.display.DisplayLabelMapper;
import com.example.techwatch.keyword.Keyword;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class KeywordTableController {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();
    private final KeywordPreferenceService preferences = new KeywordPreferenceService();
    private final VBox root = new VBox(12);
    private final TableView<Keyword> table = new TableView<>();
    private final CheckBox learningOnly = new CheckBox("学習中のみ表示");
    private final CheckBox pinnedOnly = new CheckBox("固定のみ表示");
    private List<Keyword> allKeywords = List.of();
    private Consumer<List<Keyword>> onChanged = ignored -> { };

    public KeywordTableController() {
        root.setPadding(new Insets(20));
        Label heading = new Label("キーワードの判断");
        heading.getStyleClass().add("section-title");
        Label guidance = new Label("流行度＝今週の話題量　安定度＝継続性　バズリスク＝話題先行の可能性。基礎・学習中・固定は別々の意味です。");
        guidance.setWrapText(true);
        guidance.getStyleClass().add("guidance");

        Button learning = new Button("学習中にする");
        Button stopLearning = new Button("学習中を解除");
        Button pin = new Button("ピン止め");
        Button unpin = new Button("固定解除");
        Button editReason = new Button("固定理由を編集");
        learning.setOnAction(event -> setLearning(true));
        stopLearning.setOnAction(event -> setLearning(false));
        pin.setOnAction(event -> setPinned(true, true));
        unpin.setOnAction(event -> setPinned(false, false));
        editReason.setOnAction(event -> editPinReason());
        learningOnly.setOnAction(event -> applyFilter());
        pinnedOnly.setOnAction(event -> applyFilter());
        HBox actions = new HBox(8, learning, stopLearning, pin, unpin, editReason, learningOnly, pinnedOnly);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("学習中", 70, k -> k.isLearning() ? "学習中" : ""));
        table.getColumns().add(column("固定", 55, k -> k.isPinned() ? "📌" : ""));
        table.getColumns().add(column("キーワード", 155, Keyword::getName));
        table.getColumns().add(column("状態", 90, k -> labels.keywordStatus(k.getStatus())));
        table.getColumns().add(column("流行度", 70, k -> number(k.getTrendScore())));
        table.getColumns().add(column("安定度", 70, k -> number(k.getStabilityScore())));
        table.getColumns().add(column("バズリスク", 85, k -> number(k.getBuzzRiskScore())));
        table.getColumns().add(column("判断", 220, this::recommendation));
        root.getChildren().addAll(heading, guidance, actions, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    public Node view() { return root; }
    public void setOnKeywordsChanged(Consumer<List<Keyword>> listener) { onChanged = listener == null ? ignored -> { } : listener; }
    public void update(List<Keyword> keywords) { allKeywords = List.copyOf(keywords); applyFilter(); }

    private void applyFilter() {
        table.getItems().setAll(allKeywords.stream()
                .filter(k -> !learningOnly.isSelected() || k.isLearning())
                .filter(k -> !pinnedOnly.isSelected() || k.isPinned()).toList());
    }

    private void setLearning(boolean enabled) {
        Keyword selected = selected();
        if (selected == null) return;
        try {
            replace(preferences.setLearning(selected.getId(), enabled, enabled ? "キーワード画面から学習中に指定" : ""));
        } catch (Exception error) { showError(error); }
    }

    private void setPinned(boolean enabled, boolean askReason) {
        Keyword selected = selected();
        if (selected == null) return;
        String reason = selected.getPinReason();
        if (enabled && askReason) {
            Optional<String> input = reasonDialog(selected, reason);
            if (input.isEmpty()) return;
            reason = input.get();
        }
        try { replace(preferences.setPinned(selected.getId(), enabled, reason)); }
        catch (Exception error) { showError(error); }
    }

    private void editPinReason() {
        Keyword selected = selected();
        if (selected == null) return;
        if (!selected.isPinned()) { setPinned(true, true); return; }
        Optional<String> input = reasonDialog(selected, selected.getPinReason());
        if (input.isEmpty()) return;
        try { replace(preferences.setPinned(selected.getId(), true, input.get())); }
        catch (Exception error) { showError(error); }
    }

    private Optional<String> reasonDialog(Keyword keyword, String current) {
        TextInputDialog dialog = new TextInputDialog(current);
        dialog.setTitle("固定理由");
        dialog.setHeaderText(keyword.getName() + " を継続監視する理由");
        dialog.setContentText("理由（空欄でも保存できます）:");
        return dialog.showAndWait();
    }

    private void replace(List<Keyword> keywords) {
        allKeywords = List.copyOf(keywords);
        applyFilter();
        onChanged.accept(allKeywords);
    }

    private Keyword selected() {
        Keyword keyword = table.getSelectionModel().getSelectedItem();
        if (keyword == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "操作するキーワードを一覧から選んでください。");
            alert.setHeaderText("キーワードが未選択です");
            alert.showAndWait();
        }
        return keyword;
    }

    private void showError(Exception error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, error.getMessage());
        alert.setHeaderText("キーワード設定を保存できませんでした");
        alert.showAndWait();
    }

    private TableColumn<Keyword, String> column(String title, double width, Function<Keyword, String> value) {
        TableColumn<Keyword, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private String recommendation(Keyword keyword) {
        if (keyword.isLearning()) return "継続学習";
        return switch (keyword.getStatus()) {
            case "Core" -> "基礎として維持";
            case "Watch" -> "継続監視・次の学習候補";
            case "Buzz" -> "追うが固執しない";
            case "Decline" -> "優先度を下げる";
            case "Ignore" -> keyword.isPinned() ? "固定対象として確認" : "対象外";
            default -> "必要時に確認";
        };
    }

    private String number(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.1f", value); }
}
