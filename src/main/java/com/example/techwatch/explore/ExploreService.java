package com.example.techwatch.explore;

import com.example.techwatch.article.Article;
import com.example.techwatch.db.DiscoveredKeywordRepository;
import com.example.techwatch.keyword.Keyword;
import com.example.techwatch.summarize.ArticleSummary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExploreService {
    private static final Map<String, Definition> CATALOG = catalog();
    private final DiscoveredKeywordRepository repository;

    public ExploreService(DiscoveredKeywordRepository repository) { this.repository = repository; }

    public List<DiscoveredKeyword> discover(List<Article> articles, Map<Long, ArticleSummary> summaries,
                                            List<Keyword> registered) throws Exception {
        Set<String> known = new LinkedHashSet<>();
        registered.forEach(keyword -> known.add(keyword.getNormalizedName()));
        for (Article article : articles) {
            ArticleSummary summary = summaries.get(article.getId());
            String text = (article.getTitle() + " " + article.getSummaryOriginal() + " "
                    + (summary == null ? "" : summary.shortSummary())).toLowerCase(Locale.ROOT);
            Map<String, Definition> candidates = new LinkedHashMap<>();
            CATALOG.forEach((name, definition) -> {
                if (text.contains(name.toLowerCase(Locale.ROOT))) candidates.put(name, definition);
            });
            if (summary != null) {
                for (String value : summary.relatedKeywords()) {
                    String name = clean(value);
                    if (valid(name)) candidates.putIfAbsent(name, definition(name));
                }
            }
            for (var candidate : candidates.entrySet()) {
                String normalized = candidate.getKey().toLowerCase(Locale.ROOT);
                if (known.contains(normalized) || known.stream().anyMatch(normalized::startsWith)) continue;
                Definition definition = candidate.getValue();
                repository.saveMention(new DiscoveredKeyword(null, candidate.getKey(), normalized,
                        definition.category(), definition.description(), definition.judgement(),
                        definition.prerequisites(), null, null, 0, false), article.getId(), "ARTICLE_OR_AI");
            }
        }
        return repository.findAllActive();
    }

    private String clean(String value) { return value == null ? "" : value.replaceAll("[\\r\\n\\t]", " ").trim(); }
    private boolean valid(String value) {
        return value.length() >= 2 && value.length() <= 60 && value.matches(".*[A-Za-zぁ-んァ-ヶ一-龠].*");
    }
    private Definition definition(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("ai") || lower.contains("llm") || lower.contains("vector"))
            return new Definition("AI", name + "はAI分野の記事で言及された技術語です。用途と前提を確認してから深掘りします。", "NAME_ONLY", List.of("API", "AIの基礎"));
        if (lower.contains("cloud") || lower.contains("serverless") || lower.contains("edge"))
            return new Definition("Cloud", name + "はクラウド・インフラ分野で使われる技術語です。", "LATER", List.of("ネットワーク", "Linux", "クラウド基礎"));
        return new Definition("Development Tools", name + "は今週の記事から見つかった技術語です。まず役割と利用場面を把握します。", "NAME_ONLY", List.of("Web API", "開発基礎"));
    }

    private static Map<String, Definition> catalog() {
        Map<String, Definition> values = new LinkedHashMap<>();
        values.put("OpenTelemetry", new Definition("DevOps", "ログ、メトリクス、トレースを標準化して収集する仕組みです。", "LATER", List.of("Web API", "ログ", "Docker")));
        values.put("Vector Database", new Definition("AI / Data Engineering", "文章や画像をベクトルとして保存し、意味の近さで検索するデータベースです。", "NAME_ONLY", List.of("SQL", "API", "LLM", "検索")));
        values.put("WASI", new Definition("Infrastructure", "WebAssemblyをブラウザ外で安全に動かすためのシステムインターフェースです。", "NAME_ONLY", List.of("WebAssembly", "OS基礎")));
        values.put("LangGraph", new Definition("AI", "状態を持つLLMワークフローをグラフとして組み立てるための仕組みです。", "LATER", List.of("Python", "LLM", "RAG")));
        values.put("Lakehouse", new Definition("Data Engineering", "データレイクとデータウェアハウスの特徴を統合するデータ基盤設計です。", "LATER", List.of("SQL", "データレイク", "ETL")));
        values.put("eBPF", new Definition("Infrastructure / Security", "Linuxカーネル内で安全に処理を動かし、監視やネットワーク制御に使う技術です。", "LATER", List.of("Linux", "ネットワーク", "OS基礎")));
        values.put("Observability", new Definition("DevOps", "ログ・メトリクス・トレースからシステム内部の状態を理解する考え方です。", "LATER", List.of("ログ", "監視", "Web API")));
        values.put("SRE", new Definition("DevOps", "信頼性をソフトウェア工学で改善する運用の考え方と実践です。", "LATER", List.of("運用監視", "Linux", "クラウド基礎")));
        values.put("Serverless", new Definition("Cloud", "サーバー管理を意識せず、実行量に応じて処理を動かすクラウド方式です。", "NAME_ONLY", List.of("クラウド基礎", "Web API")));
        values.put("Edge Computing", new Definition("Cloud / IoT", "利用者や端末に近い場所で処理し、遅延や通信量を減らす方式です。", "NAME_ONLY", List.of("ネットワーク", "クラウド基礎")));
        return values;
    }

    private record Definition(String category, String description, String judgement, List<String> prerequisites) { }
}
