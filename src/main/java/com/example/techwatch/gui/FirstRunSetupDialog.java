package com.example.techwatch.gui;

import com.example.techwatch.app.UserSetupSelection;
import com.example.techwatch.app.UserSetupService;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FirstRunSetupDialog {
    private static final List<String> LEARNING_CHOICES = List.of(
            "Java", "Python", "JavaScript", "TypeScript", "React", "Spring Boot", "SQL", "Linux",
            "Docker", "Git", "HTTP", "AWS", "Kubernetes");
    private static final List<String> PINNED_CHOICES = List.of(
            "AI Agent", "LLM", "RAG", "MCP", "Databricks", "Kubernetes", "Cloudflare Workers",
            "Kafka", "Security", "IoT");
    private static final Map<String, String> CATEGORY_CHOICES = new LinkedHashMap<>();
    static {
        CATEGORY_CHOICES.put("Backend", "バックエンド");
        CATEGORY_CHOICES.put("Frontend", "フロントエンド");
        CATEGORY_CHOICES.put("Infrastructure", "インフラ");
        CATEGORY_CHOICES.put("Cloud", "クラウド");
        CATEGORY_CHOICES.put("AI", "AI");
        CATEGORY_CHOICES.put("Data Engineering", "データ基盤");
        CATEGORY_CHOICES.put("Security", "セキュリティ");
        CATEGORY_CHOICES.put("IoT", "IoT");
        CATEGORY_CHOICES.put("Mobile", "モバイル");
        CATEGORY_CHOICES.put("Game Development", "ゲーム開発");
    }

    private final UserSetupService service = new UserSetupService();

    public boolean show(Window owner, boolean firstRun) {
        try {
            UserSetupSelection current = service.load();
            Dialog<UserSetupSelection> dialog = new Dialog<>();
            dialog.initOwner(owner);
            dialog.setTitle("TechWatch 初期設定");
            dialog.setHeaderText(firstRun ? "あなた向けの週報を作るため、最初に3つだけ教えてください。"
                    : "学習中・固定・興味領域を見直します。");
            ButtonType save = new ButtonType("保存して開始", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

            TextField displayName = new TextField(current.displayName());
            displayName.setPromptText("表示名（任意）");
            TextArea goal = new TextArea(current.primaryGoal());
            goal.setPromptText("例: Javaバックエンドからクラウド・AI基盤へ広げたい");
            goal.setPrefRowCount(2);
            goal.setWrapText(true);

            Map<String, CheckBox> learning = checkBoxes(LEARNING_CHOICES, current.learningKeywords());
            Map<String, CheckBox> pinned = checkBoxes(PINNED_CHOICES, current.pinnedKeywords());
            Map<String, CheckBox> categories = categoryCheckBoxes(current.interestCategories());
            TextField otherLearning = new TextField();
            otherLearning.setPromptText("その他の学習中技術（カンマ区切り）");
            TextField otherPinned = new TextField();
            otherPinned.setPromptText("その他の固定キーワード（カンマ区切り）");

            VBox content = new VBox(10,
                    sectionTitle("プロフィール（任意）"), displayName, goal,
                    sectionTitle("1. 現在、基礎として学習中の技術"), flow(learning), otherLearning,
                    sectionTitle("2. 継続的に情報収集したい技術"), flow(pinned), otherPinned,
                    sectionTitle("3. 興味のある領域"), flow(categories));
            content.setPadding(new Insets(10));
            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportWidth(720);
            scroll.setPrefViewportHeight(580);
            dialog.getDialogPane().setContent(scroll);
            dialog.getDialogPane().setPrefWidth(760);

            dialog.setResultConverter(button -> {
                if (button != save) return null;
                Set<String> learningValues = selected(learning);
                learningValues.addAll(customValues(otherLearning.getText()));
                Set<String> pinnedValues = selected(pinned);
                pinnedValues.addAll(customValues(otherPinned.getText()));
                return new UserSetupSelection(displayName.getText(), goal.getText(), learningValues,
                        pinnedValues, selected(categories));
            });

            Optional<UserSetupSelection> result = dialog.showAndWait();
            if (result.isEmpty()) return false;
            service.save(result.get());
            return true;
        } catch (Exception error) {
            Alert alert = new Alert(Alert.AlertType.ERROR, error.getMessage());
            alert.initOwner(owner);
            alert.setHeaderText("初期設定を保存できませんでした");
            alert.showAndWait();
            return false;
        }
    }

    private Map<String, CheckBox> checkBoxes(List<String> values, Set<String> selected) {
        Map<String, CheckBox> boxes = new LinkedHashMap<>();
        for (String value : values) {
            CheckBox box = new CheckBox(value);
            box.setSelected(containsIgnoreCase(selected, value));
            boxes.put(value, box);
        }
        // Preserve custom values when reopening settings.
        for (String value : selected) {
            if (!containsIgnoreCase(boxes.keySet(), value)) {
                CheckBox box = new CheckBox(value);
                box.setSelected(true);
                boxes.put(value, box);
            }
        }
        return boxes;
    }

    private Map<String, CheckBox> categoryCheckBoxes(Set<String> selected) {
        Map<String, CheckBox> boxes = new LinkedHashMap<>();
        CATEGORY_CHOICES.forEach((internal, display) -> {
            CheckBox box = new CheckBox(display);
            box.setSelected(containsIgnoreCase(selected, internal));
            boxes.put(internal, box);
        });
        return boxes;
    }

    private FlowPane flow(Map<String, CheckBox> boxes) {
        FlowPane flow = new FlowPane(12, 10);
        flow.getChildren().addAll(boxes.values());
        return flow;
    }

    private Label sectionTitle(String value) {
        Label label = new Label(value);
        label.getStyleClass().add("subsection-title");
        VBox.setMargin(label, new Insets(12, 0, 0, 0));
        return label;
    }

    private Set<String> selected(Map<String, CheckBox> boxes) {
        Set<String> result = new LinkedHashSet<>();
        boxes.forEach((value, box) -> { if (box.isSelected()) result.add(value); });
        return result;
    }

    private Set<String> customValues(String text) {
        Set<String> values = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return values;
        for (String value : text.split("[,、]")) if (!value.isBlank()) values.add(value.trim());
        return values;
    }

    private boolean containsIgnoreCase(Set<String> values, String target) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(target));
    }
}
