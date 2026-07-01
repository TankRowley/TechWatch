package com.example.techwatch.mail;

import com.example.techwatch.app.WeeklyRunResult;
import com.example.techwatch.article.Article;
import com.example.techwatch.display.JapaneseSummaryFormatter;
import com.example.techwatch.summarize.ArticleSummary;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class GmailComposeService {
    public GmailDraft create(String recipient, WeeklyRunResult result) {
        if (recipient == null || !recipient.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("送信先メールアドレスを入力してください");
        }
        if (result == null) throw new IllegalArgumentException("先に週報を生成してください");

        List<Article> mustRead = byLabel(result.articles(), "Must Read", 5);
        List<Article> watch = byLabel(result.articles(), "Watch", 5);
        String subject = "【てっくにゅーす】必読" + mustRead.size() + "件・注視" + watch.size()
                + "件（" + LocalDate.now() + "）";
        StringBuilder body = new StringBuilder("てっくにゅーす 週間ダイジェスト\n\n");
        body.append("今週は").append(result.articles().size()).append("件を評価しました。\n\n");
        appendSection(body, "必読記事", mustRead, result);
        appendSection(body, "注視する記事", watch, result);
        body.append("詳しい評価は、てっくにゅーすの「週報」タブで確認できます。\n");

        String query = "view=cm&fs=1&to=" + encode(recipient.trim()) + "&su=" + encode(subject)
                + "&body=" + encode(body.toString());
        return new GmailDraft(subject, body.toString(), "https://mail.google.com/mail/?" + query);
    }

    private List<Article> byLabel(List<Article> articles, String label, int limit) {
        return articles.stream().filter(article -> label.equals(article.getImportanceLabel())).limit(limit).toList();
    }

    private void appendSection(StringBuilder out, String heading, List<Article> articles, WeeklyRunResult result) {
        out.append("■ ").append(heading).append("\n\n");
        if (articles.isEmpty()) {
            out.append("該当記事はありません。\n\n");
            return;
        }
        for (int i = 0; i < articles.size(); i++) {
            Article article = articles.get(i);
            ArticleSummary summary = result.summaries().get(article.getId());
            out.append(i + 1).append(". ").append(article.getTitle()).append("\n")
                    .append(shorten(JapaneseSummaryFormatter.visibleSummary(summary), 180)).append("\n");
            if (summary != null && !summary.whyItMatters().isBlank()) {
                out.append("読む理由: ").append(shorten(summary.whyItMatters(), 120)).append("\n");
            }
            out.append(article.getUrl()).append("\n\n");
        }
    }

    private String shorten(String value, int maximum) {
        if (value == null || value.length() <= maximum) return value == null ? "" : value;
        return value.substring(0, maximum - 1) + "…";
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    public record GmailDraft(String subject, String body, String composeUrl) { }
}
