package com.example.techwatch.report;

import com.example.techwatch.article.Article;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class MarkdownReportWriter {
    public Path write(WeeklyReport report, Path directory) throws IOException {
        Files.createDirectories(directory);
        Path output = directory.resolve(report.periodEnd() + ".md");
        Files.writeString(output, render(report), StandardCharsets.UTF_8);
        return output;
    }

    public String render(WeeklyReport report) {
        StringBuilder out = new StringBuilder();
        out.append("# TechWatch Weekly Report\n\n期間: ").append(report.periodStart()).append(" 〜 ")
                .append(report.periodEnd()).append("\n\n");
        out.append("## 1. 今週の結論\n\n").append(conclusion(report)).append("\n\n");
        articleSection(out, "## 2. Must Read", report, "Must Read");
        articleSection(out, "## 3. Watch", report, "Watch");
        articleSection(out, "## 4. Skim", report, "Skim");
        out.append("## 5. 今週伸びたキーワード\n\n");
        out.append("| Keyword | Status | Trend | Stability | Buzz Risk | Recommendation |\n")
                .append("|---|---|---:|---:|---:|---|\n");
        report.keywords().stream().filter(k -> k.getTrendScore() > 0)
                .sorted(Comparator.comparingDouble(Keyword::getTrendScore).reversed()).limit(15)
                .forEach(k -> out.append("| ").append(escape(k.getName())).append(" | ").append(k.getStatus())
                        .append(" | ").append(number(k.getTrendScore())).append(" | ").append(number(k.getStabilityScore()))
                        .append(" | ").append(number(k.getBuzzRiskScore())).append(" | ")
                        .append(recommendation(k)).append(" |\n"));
        if (report.keywords().stream().noneMatch(k -> k.getTrendScore() > 0)) out.append("| - | - | 0 | 0 | 0 | 今週の検出なし |\n");
        out.append("\n## 6. Buzz疑い\n\n");
        keywordBullets(out, report.keywords().stream().filter(k -> "Buzz".equalsIgnoreCase(k.getStatus())).toList(), "Buzz候補はありません。");
        out.append("\n## 7. Core技術の動き\n\n");
        keywordBullets(out, report.keywords().stream().filter(k -> "Core".equalsIgnoreCase(k.getStatus()) && k.getTrendScore() > 0).toList(),
                "今週、Core技術の新しい言及は検出されませんでした。");
        out.append("\n## 8. 今の自分が学ぶべきこと\n\n");
        List<Keyword> learning = report.keywords().stream()
                .filter(k -> ("Core".equalsIgnoreCase(k.getStatus()) || "Watch".equalsIgnoreCase(k.getStatus())) && k.getFinalScore() > 0)
                .sorted(Comparator.comparingDouble(Keyword::getFinalScore).reversed()).limit(5).toList();
        if (learning.isEmpty()) out.append("1. HTTP / Linux / Docker / SQL の基礎を継続する。\n");
        else for (int i = 0; i < learning.size(); i++) out.append(i + 1).append(". ").append(learning.get(i).getName()).append("\n");
        out.append("\n## 9. 後回しでよいこと\n\n");
        List<Keyword> later = report.keywords().stream().filter(k -> "Buzz".equalsIgnoreCase(k.getStatus()) || "Decline".equalsIgnoreCase(k.getStatus())).limit(8).toList();
        keywordBullets(out, later, "明確な後回し候補はありません。");
        return out.toString();
    }

    private String conclusion(WeeklyReport report) {
        long mustRead = report.articles().stream().filter(a -> "Must Read".equals(a.getImportanceLabel())).count();
        List<String> rising = report.keywords().stream().filter(k -> k.getTrendScore() > 0)
                .sorted(Comparator.comparingDouble(Keyword::getTrendScore).reversed()).limit(3).map(Keyword::getName).toList();
        if (report.articles().isEmpty()) return "今週の対象記事はありません。情報源の取得状況を確認し、基礎学習を継続します。";
        return "今週は" + report.articles().size() + "件を評価し、Must Readは" + mustRead + "件でした。"
                + (rising.isEmpty() ? "" : " 注目キーワードは " + String.join(" / ", rising) + " です。")
                + " 話題性だけでなく、Core技術との接続を優先して読みます。";
    }

    private void articleSection(StringBuilder out, String heading, WeeklyReport report, String label) {
        out.append(heading).append("\n\n");
        List<Article> articles = report.articles().stream().filter(a -> label.equals(a.getImportanceLabel())).limit(10).toList();
        if (articles.isEmpty()) { out.append("該当記事はありません。\n\n"); return; }
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            ArticleSummary summary = report.summaries().get(article.getId());
            out.append("### ").append(i + 1).append(". ").append(escape(article.getTitle())).append("\n\n")
                    .append("Score: ").append(number(article.getArticleScore())).append("  \n")
                    .append("Source: ").append(escape(article.getSourceName())).append("  \n")
                    .append("URL: ").append(article.getUrl()).append("\n\nSummary:\n")
                    .append(plain(summary == null ? article.getSummaryOriginal() : summary.shortSummary())).append("\n\n");
            if (summary != null && !summary.whyItMatters().isBlank()) {
                out.append("Why it matters:\n").append(plain(summary.whyItMatters())).append("\n\n")
                        .append("Learning Priority: ").append(summary.learningPriority()).append("\n\n");
            }
        }
    }

    private void keywordBullets(StringBuilder out, List<Keyword> keywords, String empty) {
        if (keywords.isEmpty()) { out.append(empty).append("\n"); return; }
        keywords.forEach(k -> out.append("- **").append(escape(k.getName())).append("** — ").append(recommendation(k)).append("\n"));
    }

    private String recommendation(Keyword keyword) {
        return switch (keyword.getStatus()) {
            case "Core" -> "基礎として継続学習";
            case "Watch" -> "継続監視";
            case "Buzz" -> "追跡するが固執しない";
            case "Decline" -> "優先度を下げる";
            default -> "必要時のみ確認";
        };
    }

    private String plain(String value) { return value == null || value.isBlank() ? "（概要なし）" : Jsoup.parse(value).text(); }
    private String escape(String value) { return value == null ? "" : value.replace("|", "\\|").replace("\n", " "); }
    private String number(double value) { return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.1f", value); }
}
