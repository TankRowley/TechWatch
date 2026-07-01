package com.example.techwatch.report;

import com.example.techwatch.article.Article;
import com.example.techwatch.display.DisplayLabelMapper;
import com.example.techwatch.display.JapaneseSummaryFormatter;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;
import com.example.techwatch.explore.DiscoveredKeyword;
import com.example.techwatch.market.KeywordMarketStats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class MarkdownReportWriter {
    private final DisplayLabelMapper labels = new DisplayLabelMapper();

    public Path write(WeeklyReport report, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path output = directory.resolve(report.periodEnd() + ".md");
        Files.writeString(output, render(report), StandardCharsets.UTF_8);
        return output;
    }

    public String render(WeeklyReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# てっくにゅーす 週間レポート\n\n期間: ").append(report.periodStart()).append(" 〜 ")
                .append(report.periodEnd()).append("\n\n");
        out.append("## 1. 今週の結論\n\n").append(conclusion(report)).append("\n\n");
        articleSection(out, "## 2. 必読記事", report, "Must Read");
        articleSection(out, "## 3. 注視する記事", report, "Watch");
        articleSection(out, "## 4. 軽く確認する記事", report, "Skim");
        pinnedSection(out, report);
        learningSection(out, report);
        trendSection(out, report);
        out.append("## 8. 流行疑いのキーワード\n\n");
        keywordBullets(out, report.keywords().stream().filter(k -> "Buzz".equalsIgnoreCase(k.getStatus())).toList(),
                "流行疑いのキーワードはありません。");
        out.append("\n## 9. IT全体の地図 — 基礎技術\n\n");
        keywordBullets(out, report.keywords().stream()
                .filter(Keyword::isFoundation)
                .sorted(Comparator.comparingDouble(Keyword::getActivityScore).reversed()).toList(),
                "基礎技術はまだ設定されていません。");
        out.append("\n## 10. 今の自分が学ぶべきこと\n\n");
        List<Keyword> priorities = report.keywords().stream()
                .filter(k -> k.isLearning() || k.isPinned())
                .sorted(Comparator.comparingDouble(Keyword::getFinalScore).reversed()).limit(8).toList();
        if (priorities.isEmpty()) out.append("初期設定で学習中または固定キーワードを選ぶと、ここに優先順位が表示されます。\n");
        else for (int i = 0; i < priorities.size(); i++) {
            Keyword keyword = priorities.get(i);
            out.append(i + 1).append(". ").append(keyword.getName()).append(" — ").append(recommendation(keyword)).append("\n");
        }
        out.append("\n## 11. 後回しでよいこと\n\n");
        List<Keyword> later = report.keywords().stream()
                .filter(k -> !k.isPinned() && !k.isLearning()
                        && ("Buzz".equalsIgnoreCase(k.getStatus()) || "Decline".equalsIgnoreCase(k.getStatus())))
                .limit(8).toList();
        keywordBullets(out, later, "明確な後回し候補はありません。");
        marketSection(out, report);
        exploreSection(out, report);
        coolingSection(out, report);
        return out.toString();
    }

    private void pinnedSection(StringBuilder out, WeeklyReport report) {
        out.append("## 5. 固定キーワードの動き\n\n");
        List<Keyword> pinned = report.keywords().stream().filter(Keyword::isPinned)
                .sorted(Comparator.comparingDouble(Keyword::getTrendScore).reversed()).toList();
        if (pinned.isEmpty()) {
            out.append("固定キーワードはまだありません。キーワード画面から継続監視したい技術を固定できます。\n\n");
            return;
        }
        for (Keyword keyword : pinned) {
            out.append("### ").append(escape(keyword.getName())).append("\n\n")
                    .append("状態: ").append(labels.keywordStatus(keyword.getStatus())).append("  \n")
                    .append("今週の記事数: ").append(number(keyword.getTrendScore())).append("  \n")
                    .append("判断: ").append(recommendation(keyword)).append("  \n")
                    .append("理由: ").append(keyword.getPinReason().isBlank() ? automaticReason(keyword) : keyword.getPinReason())
                    .append("\n\n");
        }
    }

    private void learningSection(StringBuilder out, WeeklyReport report) {
        out.append("## 6. 学習中キーワードに関係する記事\n\n");
        List<Keyword> learning = report.keywords().stream().filter(Keyword::isLearning)
                .sorted(Comparator.comparingDouble(Keyword::getTrendScore).reversed()).toList();
        if (learning.isEmpty()) {
            out.append("学習中キーワードはまだありません。初期設定またはキーワード画面から指定できます。\n\n");
            return;
        }
        for (Keyword keyword : learning) {
            out.append("### ").append(escape(keyword.getName())).append("\n\n")
                    .append("今週の記事数: ").append(number(keyword.getTrendScore())).append("  \n")
                    .append("判断: 継続学習対象  \n")
                    .append("コメント: ").append(learningComment(keyword)).append("\n\n");
        }
    }

    private void trendSection(StringBuilder out, WeeklyReport report) {
        out.append("## 7. 今週伸びたキーワード\n\n")
                .append("| キーワード | 状態 | 最近の動き | 今週言及 | 活動度 | 長期安定 | 信頼度 | バズリスク | 判断 |\n")
                .append("|---|---|---|---:|---:|---:|---:|---:|---|\n");
        report.keywords().stream().filter(k -> k.getTrendScore() > 0)
                .sorted(Comparator.comparingDouble(Keyword::getActivityScore).reversed()).limit(15)
                .forEach(k -> out.append("| ").append(escape(k.getName())).append(" | ")
                        .append(labels.keywordStatus(k.getStatus())).append(" | ")
                        .append(labels.trendState(k.getTrendState())).append(" | ").append(number(k.getTrendScore()))
                        .append(" | ").append(number(k.getActivityScore())).append(" | ")
                        .append(number(k.getStabilityScore())).append(" | ")
                        .append(number(k.getConfidenceScore())).append(" | ")
                        .append(number(k.getBuzzRiskScore())).append(" | ").append(recommendation(k)).append(" |\n"));
        if (report.keywords().stream().noneMatch(k -> k.getTrendScore() > 0)) {
            out.append("| - | - | - | 0 | 0 | 0 | 0 | 0 | 今週の検出なし |\n");
        }
        out.append("\n");
    }

    private void marketSection(StringBuilder out, WeeklyReport report) {
        out.append("\n## 12. 求人市場シグナル\n\n");
        List<Keyword> available = report.keywords().stream()
                .filter(keyword -> {
                    KeywordMarketStats value = report.marketStats().get(keyword.getId());
                    return value != null && (value.usJobCount() > 0 || value.jpJobCount() > 0);
                })
                .sorted(Comparator.comparingDouble((Keyword keyword) ->
                        report.marketStats().get(keyword.getId()).globalMarketScore()).reversed()).limit(10).toList();
        if (available.isEmpty()) {
            out.append("求人CSVの履歴がまだありません。設定フォルダの job-market.csv に件数を追加すると評価されます。\n");
            return;
        }
        for (int i = 0; i < available.size(); i++) {
            Keyword keyword = available.get(i);
            KeywordMarketStats value = report.marketStats().get(keyword.getId());
            out.append("### ").append(i + 1).append(". ").append(escape(keyword.getName())).append("\n\n")
                    .append("市場評価: ").append(labels.marketLabel(value.marketLabel())).append("  \n")
                    .append("米国求人数: ").append(value.usJobCount()).append("  \n")
                    .append("日本求人数: ").append(value.jpJobCount()).append("  \n")
                    .append("判断: ").append(marketReason(keyword, value)).append("\n\n");
        }
    }

    private void exploreSection(StringBuilder out, WeeklyReport report) {
        out.append("## 13. 今週の未知キーワード\n\n");
        List<DiscoveredKeyword> values = report.discoveredKeywords().stream()
                .filter(value -> !"IGNORE".equals(value.learningJudgement())).limit(10).toList();
        if (values.isEmpty()) { out.append("今週の未知キーワードはまだありません。\n\n"); return; }
        for (int i = 0; i < values.size(); i++) {
            DiscoveredKeyword value = values.get(i);
            out.append("### ").append(i + 1).append(". ").append(escape(value.name())).append("\n\n")
                    .append("分類: ").append(labels.category(value.category())).append("  \n")
                    .append("判断: ").append(labels.exploreJudgement(value.learningJudgement())).append("  \n")
                    .append("説明: ").append(value.description()).append("  \n")
                    .append("前提知識: ").append(value.prerequisites().isEmpty() ? "特になし" : String.join(" / ", value.prerequisites()))
                    .append("  \nコメント: まず役割を把握し、学習中または固定への昇格はユーザーが判断します。\n\n");
        }
    }

    private void coolingSection(StringBuilder out, WeeklyReport report) {
        out.append("## 14. 減速中・休眠中のキーワード\n\n");
        List<Keyword> values = report.keywords().stream()
                .filter(keyword -> !keyword.isFoundation()
                        && ("Cooling".equals(keyword.getTrendState()) || "Dormant".equals(keyword.getTrendState())))
                .limit(12).toList();
        if (values.isEmpty()) { out.append("減速中・休眠中のキーワードはありません。\n"); return; }
        for (Keyword keyword : values) {
            out.append("### ").append(escape(keyword.getName())).append("\n\n")
                    .append("状態: ").append(labels.keywordStatus(keyword.getStatus())).append("  \n")
                    .append("最近の動き: ").append(labels.trendState(keyword.getTrendState())).append("  \n")
                    .append("判断: ").append(recommendation(keyword)).append("  \n")
                    .append("理由: ").append(historyReason(keyword)).append("\n\n");
        }
    }

    private String marketReason(Keyword keyword, KeywordMarketStats value) {
        return switch (value.marketLabel()) {
            case "Hot" -> "記事と求人の両方が強く、学習リターンが高い可能性があります。";
            case "Stable Demand" -> "派手さだけでなく実務需要があり、基礎として継続する価値があります。";
            case "US Leading" -> "米国需要が先行しています。国内動向を待ちながら監視します。";
            case "JP Strong" -> "国内求人でも需要が確認でき、実務との接続が期待できます。";
            case "Buzz Only" -> "記事の話題量に対して求人が弱いため、基礎学習より優先しすぎません。";
            case "Declining Demand" -> "求人需要が低下しているため、優先度を見直します。";
            default -> "件数の絶対値を過信せず、記事傾向と合わせて継続観察します。";
        };
    }

    private String historyReason(Keyword keyword) {
        if (keyword.isFoundation()) return "最近の記事が少なくても、基礎技術としての重要性は維持します。";
        if (keyword.isLearning()) return "話題量だけで学習対象から外さず、現在の学習計画を優先します。";
        if (keyword.isPinned()) return "ユーザーが固定監視しているため、自動では対象外にしません。";
        return "過去の履歴に比べ直近の出現が少ないため、監視優先度を下げます。";
    }

    private String conclusion(WeeklyReport report) {
        long mustRead = report.articles().stream().filter(a -> "Must Read".equals(a.getImportanceLabel())).count();
        List<String> rising = report.keywords().stream().filter(k -> "Rising".equals(k.getTrendState()))
                .sorted(Comparator.comparingDouble(Keyword::getActivityScore).reversed()).limit(3).map(Keyword::getName).toList();
        if (report.articles().isEmpty()) return "今週の対象記事はありません。情報源の取得状況を確認してください。";
        return "今週は" + report.articles().size() + "件を評価し、必読は" + mustRead + "件でした。"
                + (rising.isEmpty() ? "" : " 注目キーワードは " + String.join(" / ", rising) + " です。")
                + " 一般的な重要度だけでなく、学習中・固定・興味領域との接続を優先して読みます。";
    }

    private void articleSection(StringBuilder out, String heading, WeeklyReport report, String label) {
        out.append(heading).append("\n\n");
        List<Article> articles = report.articles().stream().filter(a -> label.equals(a.getImportanceLabel())).limit(10).toList();
        if (articles.isEmpty()) { out.append("該当記事はありません。\n\n"); return; }
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            ArticleSummary summary = report.summaries().get(article.getId());
            out.append("### ").append(i + 1).append(". ").append(escape(article.getTitle())).append("\n\n")
                    .append("評価: ").append(labels.articleLabel(article.getImportanceLabel())).append("（")
                    .append(number(article.getArticleScore())).append("点）  \n")
                    .append("情報源: ").append(escape(article.getSourceName())).append("  \n")
                    .append("リンク: ").append(article.getUrl()).append("\n\n")
                    .append("概要:\n").append(JapaneseSummaryFormatter.visibleSummary(summary)).append("\n\n");
            if (summary != null && !summary.whyItMatters().isBlank()) {
                out.append("自分に関係ある理由:\n").append(summary.whyItMatters()).append("\n\n")
                        .append("学習優先度: ").append(labels.learningPriority(summary.learningPriority())).append("\n\n");
            }
        }
    }

    private void keywordBullets(StringBuilder out, List<Keyword> keywords, String empty) {
        if (keywords.isEmpty()) { out.append(empty).append("\n"); return; }
        keywords.forEach(k -> out.append("- **").append(escape(k.getName())).append("** — ")
                .append(labels.keywordStatus(k.getStatus())).append(" / ").append(recommendation(k)).append("\n"));
    }

    private String recommendation(Keyword keyword) {
        if (keyword.isLearning()) return "継続学習";
        if (keyword.isFoundation()) return "基礎として維持";
        if ("Insufficient".equals(keyword.getTrendState())) return "データ蓄積中";
        return switch (keyword.getStatus()) {
            case "Core" -> "基礎として維持";
            case "Watch" -> "継続監視";
            case "Buzz" -> "追うが固執しない";
            case "Decline" -> "優先度を下げる";
            case "Ignore" -> keyword.isPinned() ? "固定対象として確認" : "対象外";
            default -> "必要時に確認";
        };
    }

    private String automaticReason(Keyword keyword) {
        if ("Buzz".equalsIgnoreCase(keyword.getStatus())) return "話題量を追いつつ、実装事例が増えるか確認するため。";
        if (keyword.getTrendScore() > 0) return "今週も記事で言及されており、継続監視の対象だから。";
        return "ユーザーが継続監視対象として固定しているため。";
    }

    private String learningComment(Keyword keyword) {
        if (keyword.getTrendScore() <= 0) return "今週の新しい言及は少ないため、基礎教材を優先するとよい。";
        if ("Core".equalsIgnoreCase(keyword.getStatus())) return "周辺技術の記事と結びつけながら、実務基礎として継続するとよい。";
        return "関連する高評価記事を確認し、現在の学習内容との接点を一つ試すとよい。";
    }

    private String escape(String value) { return value == null ? "" : value.replace("|", "\\|").replace("\n", " "); }
    private String number(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.1f", value); }
}
