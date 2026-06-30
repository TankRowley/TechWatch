package com.example.techwatch.summarize;

import com.example.techwatch.article.Article;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpenAiArticleSummarizer implements ArticleSummarizer {
    private static final URI RESPONSES_ENDPOINT = URI.create("https://api.openai.com/v1/responses");
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ArticleSummarizer fallback;

    public OpenAiArticleSummarizer(String apiKey, String model) {
        this(apiKey, model, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(),
                new NoopArticleSummarizer());
    }

    public OpenAiArticleSummarizer(String apiKey, String model, HttpClient httpClient, ArticleSummarizer fallback) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-5-mini" : model.trim();
        this.httpClient = httpClient;
        this.fallback = fallback;
    }

    @Override
    public ArticleSummary summarize(Article article, String bodyText) {
        if (apiKey.isBlank() || bodyText == null || bodyText.isBlank()) return fallback.summarize(article, bodyText);
        try {
            String requestBody = buildRequest(article, bodyText);
            HttpRequest request = HttpRequest.newBuilder(RESPONSES_ENDPOINT).timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return fallback.summarize(article, bodyText);
            return parseResponse(response.body());
        } catch (Exception ignored) {
            return fallback.summarize(article, bodyText);
        }
    }

    String buildRequest(Article article, String bodyText) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("instructions", """
                あなたは若手Javaバックエンドエンジニア向けの技術リサーチ補助AIです。
                記事を簡潔に要約し、長期的価値と学習順序を判断してください。
                広告的な表現を避け、記事本文から確認できる内容だけを出力してください。
                出力は必ず日本語にしてください。英語記事であっても日本語で要約してください。
                JSONのキーは英語で構いませんが、値の説明文は日本語にしてください。
                専門用語は必要に応じて英語を併記して構いません。
                返答はJSONのみとし、前置きや説明文は付けないでください。
                """);
        String body = bodyText.substring(0, Math.min(16_000, bodyText.length()));
        root.put("input", "タイトル: " + article.getTitle() + "\nURL: " + article.getUrl()
                + "\n情報源: " + article.getSourceName() + "\n本文:\n" + body);
        root.put("max_output_tokens", 900);

        ObjectNode format = root.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "techwatch_article_summary");
        format.put("strict", true);
        ObjectNode schema = format.putObject("schema");
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("summary").put("type", "string");
        stringArray(properties, "technicalPoints");
        properties.putObject("whyItMatters").put("type", "string");
        properties.putObject("learningPriority").put("type", "string")
                .putArray("enum").add("Now").add("Soon").add("Later").add("Watch Only").add("Ignore");
        stringArray(properties, "prerequisites");
        stringArray(properties, "keywords");
        properties.putObject("importanceLabel").put("type", "string")
                .putArray("enum").add("Must Read").add("Watch").add("Skim").add("Archive").add("Ignore");
        schema.putArray("required").add("summary").add("technicalPoints").add("whyItMatters")
                .add("learningPriority").add("prerequisites").add("keywords").add("importanceLabel");
        schema.put("additionalProperties", false);
        return mapper.writeValueAsString(root);
    }

    private void stringArray(ObjectNode properties, String name) {
        ObjectNode node = properties.putObject(name);
        node.put("type", "array");
        node.putObject("items").put("type", "string");
    }

    public ArticleSummary parseResponse(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        String outputText = root.path("output_text").asText("");
        if (outputText.isBlank()) {
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("output_text".equals(content.path("type").asText())) {
                        outputText = content.path("text").asText("");
                        if (!outputText.isBlank()) break;
                    }
                }
            }
        }
        if (outputText.isBlank()) throw new IllegalArgumentException("Responses APIの本文が空です");
        return parseSummaryJson(outputText);
    }

    public ArticleSummary parseSummaryJson(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        return new ArticleSummary(node.path("summary").asText(""), list(node.path("technicalPoints")),
                node.path("whyItMatters").asText(""), node.path("learningPriority").asText("Watch Only"),
                list(node.path("prerequisites")), list(node.path("keywords")),
                node.path("importanceLabel").asText("UNRATED"));
    }

    private List<String> list(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node instanceof ArrayNode) node.forEach(item -> values.add(item.asText()));
        return values;
    }
}
