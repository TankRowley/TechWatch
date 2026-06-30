package com.example.techwatch.gui;

import com.example.techwatch.keyword.KeywordWeeklyStats;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.List;

public class KeywordTrendChartController {
    private final LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
    private final Label empty = new Label("まだ十分な履歴がありません");
    private final StackPane root = new StackPane(chart, empty);

    public KeywordTrendChartController() {
        chart.setTitle("週ごとの記事出現数");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        ((CategoryAxis) chart.getXAxis()).setLabel("週");
        ((NumberAxis) chart.getYAxis()).setLabel("記事出現数");
        chart.setPrefHeight(260);
    }

    public Node view() { return root; }
    public void update(List<KeywordWeeklyStats> values) {
        chart.getData().clear();
        if (values == null || values.isEmpty()) { chart.setVisible(false); empty.setVisible(true); return; }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("記事出現数");
        values.forEach(value -> series.getData().add(new XYChart.Data<>(value.weekStart().toString(), value.mentionCount())));
        chart.getData().add(series); chart.setVisible(true); empty.setVisible(false);
    }
}
