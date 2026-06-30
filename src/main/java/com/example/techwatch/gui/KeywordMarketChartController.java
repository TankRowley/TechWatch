package com.example.techwatch.gui;

import com.example.techwatch.market.KeywordMarketStats;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.List;

public class KeywordMarketChartController {
    private final LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
    private final Label empty = new Label("まだ十分な求人履歴がありません");
    private final StackPane root = new StackPane(chart, empty);

    public KeywordMarketChartController() {
        chart.setTitle("求人市場の推移"); chart.setAnimated(false); chart.setCreateSymbols(true);
        ((CategoryAxis) chart.getXAxis()).setLabel("週");
        ((NumberAxis) chart.getYAxis()).setLabel("求人数");
        chart.setPrefHeight(260);
    }

    public Node view() { return root; }
    public void update(List<KeywordMarketStats> values) {
        chart.getData().clear();
        if (values == null || values.stream().noneMatch(value -> value.usJobCount() > 0 || value.jpJobCount() > 0)) {
            chart.setVisible(false); empty.setVisible(true); return;
        }
        XYChart.Series<String, Number> us = new XYChart.Series<>(); us.setName("米国求人数");
        XYChart.Series<String, Number> jp = new XYChart.Series<>(); jp.setName("日本求人数");
        values.forEach(value -> {
            us.getData().add(new XYChart.Data<>(value.weekStart().toString(), value.usJobCount()));
            jp.getData().add(new XYChart.Data<>(value.weekStart().toString(), value.jpJobCount()));
        });
        chart.getData().add(us); chart.getData().add(jp); chart.setVisible(true); empty.setVisible(false);
    }
}
