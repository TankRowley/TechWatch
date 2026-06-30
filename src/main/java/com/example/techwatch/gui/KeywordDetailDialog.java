package com.example.techwatch.gui;

import com.example.techwatch.display.DisplayLabelMapper;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.market.KeywordMarketStats;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class KeywordDetailDialog {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();

    public void show(Window owner, Keyword keyword, KeywordMarketStats market) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner); dialog.setTitle(keyword.getName() + " の詳細");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        VBox content = new VBox(10);
        content.setPadding(new Insets(12));
        Label title = new Label(keyword.getName()); title.getStyleClass().add("section-title");
        content.getChildren().addAll(title, new Label("状態: " + labels.keywordStatus(keyword.getStatus())),
                new Label("最近の動き: " + labels.trendState(keyword.getTrendState())),
                new Label("学習中: " + yes(keyword.isLearning()) + "　固定中: " + yes(keyword.isPinned())),
                new Label("市場評価: " + labels.marketLabel(market == null ? "Unknown" : market.marketLabel())),
                new Label("判断: " + reason(keyword, market)));
        try {
            KeywordChartDataService data = new KeywordChartDataService();
            KeywordTrendChartController trend = new KeywordTrendChartController();
            trend.update(data.trend(keyword.getId())); content.getChildren().add(trend.view());
            KeywordMarketChartController jobs = new KeywordMarketChartController();
            jobs.update(data.market(keyword.getId())); content.getChildren().add(jobs.view());
            Label relatedTitle = new Label("関連記事"); relatedTitle.getStyleClass().add("subsection-title");
            ListView<String> related = new ListView<>(); related.getItems().setAll(data.relatedArticles(keyword.getId()));
            related.setPrefHeight(180); related.setPlaceholder(new Label("関連記事はまだありません"));
            content.getChildren().addAll(relatedTitle, related);
        } catch (Exception error) {
            content.getChildren().add(new Label("グラフを読み込めませんでした: " + error.getMessage()));
        }
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(820); scroll.setPrefViewportHeight(680);
        dialog.getDialogPane().setContent(scroll); dialog.getDialogPane().setPrefWidth(860);
        dialog.showAndWait();
    }

    private String yes(boolean value) { return value ? "はい" : "いいえ"; }
    private String reason(Keyword keyword, KeywordMarketStats market) {
        if (keyword.isLearning()) return "現在の学習計画を優先し、履歴と求人需要を補助材料にします。";
        if (market != null && "Buzz Only".equals(market.marketLabel())) return "追いますが、基礎学習より優先しすぎません。";
        if (market != null && market.globalMarketScore() >= 55) return "求人需要との接続があるため、学習候補として確認します。";
        if ("Dormant".equals(keyword.getTrendState())) return "最近の出現は少ないため、必要時に確認します。";
        return "記事履歴と市場シグナルを合わせて継続監視します。";
    }
}
