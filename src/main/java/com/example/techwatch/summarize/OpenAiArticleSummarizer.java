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
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private final String apiKey;
    private final String model;
    private final URI apiEndpoint;
    private final boolean chatCompletions;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ArticleSummarizer fallback;

    public OpenAiArticleSummarizer(String apiKey, String model) {
        this(apiKey, model, null,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(),
                new NoopArticleSummarizer());
    }

    public OpenAiArticleSummarizer(String apiKey, String model, HttpClient httpClient, ArticleSummarizer fallback) {
        this(apiKey, model, null, httpClient, fallback);
    }

    public OpenAiArticleSummarizer(String apiKey, String model, String baseUrl) {
        this(apiKey, model, baseUrl, compatibleHttpClient(baseUrl),
                new NoopArticleSummarizer());
    }

    private static HttpClient compatibleHttpClient(String baseUrl) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15));
        if (baseUrl != null && !baseUrl.isBlank()) builder.version(HttpClient.Version.HTTP_1_1);
        return builder.build();
    }

    OpenAiArticleSummarizer(String apiKey, String model, String baseUrl, HttpClient httpClient,
                            ArticleSummarizer fallback) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-5-mini" : model.trim();
        String customEndpoint = baseUrl == null ? "" : baseUrl.trim();
        this.chatCompletions = !customEndpoint.isBlank() && !stripTrailingSlash(customEndpoint).endsWith("/responses");
        this.apiEndpoint = chatCompletions ? chatCompletionsEndpoint(customEndpoint) : responsesEndpoint(customEndpoint);
        this.httpClient = httpClient;
        this.fallback = fallback;
    }

    @Override
    public ArticleSummary summarize(Article article, String bodyText) {
        if (bodyText == null || bodyText.isBlank()) return fallback.summarize(article, bodyText);
        try {
            String requestBody = chatCompletions ? buildChatRequest(article, bodyText) : buildRequest(article, bodyText);
            HttpRequest.Builder request = HttpRequest.newBuilder(apiEndpoint).timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json");
            if (!apiKey.isBlank()) request.header("Authorization", "Bearer " + apiKey);
            HttpResponse<String> response = httpClient.send(
                    request.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return fallback.summarize(article, bodyText);
            return chatCompletions ? parseChatResponse(response.body()) : parseResponse(response.body());
        } catch (Exception ignored) {
            return fallback.summarize(article, bodyText);
        }
    }

    static URI responsesEndpoint(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        value = stripTrailingSlash(value);
        if (!value.endsWith("/responses")) value += "/responses";
        return httpEndpoint(value);
    }

    static URI chatCompletionsEndpoint(String baseUrl) {
        String value = stripTrailingSlash(baseUrl);
        if (!value.endsWith("/chat/completions")) value += "/chat/completions";
        return httpEndpoint(value);
    }

    private static URI httpEndpoint(String value) {
        URI endpoint = URI.create(value);
        String scheme = endpoint.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("OPENAI_BASE_URLはhttpまたはhttpsで指定してください");
        }
        return endpoint;
    }

    private static String stripTrailingSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    String buildRequest(Article article, String bodyText) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("instructions", instructions());
        root.put("input", articleInput(article, bodyText));
        root.put("max_output_tokens", 900);

        ObjectNode format = root.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", "techwatch_article_summary");
        format.put("strict", true);
        format.set("schema", summarySchema());
        return mapper.writeValueAsString(root);
    }

    String buildChatRequest(Article article, String bodyText) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "system").put("content", localInstructions());
        messages.addObject().put("role", "user").put("content", localArticleInput(article, bodyText));
        root.put("max_tokens", 500);
        root.put("temperature", 0.1);
        ObjectNode jsonSchema = root.putObject("response_format");
        jsonSchema.put("type", "json_schema");
        ObjectNode format = jsonSchema.putObject("json_schema");
        format.put("name", "techwatch_article_summary");
        format.put("strict", true);
        format.set("schema", summarySchema());
        return mapper.writeValueAsString(root);
    }

    private String instructions() {
        return """
                あなたは若手Javaバックエンドエンジニア向けの技術リサーチ補助AIです。
                記事を簡潔に要約し、長期的価値と学習順序を判断してください。
                広告的な表現を避け、記事本文から確認できる内容だけを出力してください。
                出力は必ず日本語にしてください。英語記事であっても日本語で要約してください。
                JSONのキーは英語で構いませんが、値の説明文は日本語にしてください。
                専門用語は必要に応じて英語を併記して構いません。
                返答はJSONのみとし、前置きや説明文は付けないでください。
                """;
    }

    private String localInstructions() {
        return "Return JSON only. Use short Japanese values.";
    }

    private String articleInput(Article article, String bodyText) {
        String body = bodyText.substring(0, Math.min(16_000, bodyText.length()));
        return "タイトル: " + article.getTitle() + "\nURL: " + article.getUrl()
                + "\n情報源: " + article.getSourceName() + "\n本文:\n" + body;
    }

    private String localArticleInput(Article article, String bodyText) {
        String body = bodyText.substring(0, Math.min(16_000, bodyText.length()));
        return "Summarize this technical article for a junior Java backend engineer.\nTitle: "
                + article.getTitle() + "\nURL: " + article.getUrl() + "\nSource: "
                + article.getSourceName() + "\nArticle:\n" + body;
    }

    private ObjectNode summarySchema() {
        ObjectNode schema = mapper.createObjectNode();
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
        return schema;
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

    public ArticleSummary parseChatResponse(String responseBody) throws Exception {
        JsonNode message = mapper.readTree(responseBody).path("choices").path(0).path("message");
        String outputText = message.path("content").asText("");
        if (outputText.isBlank()) outputText = message.path("reasoning_content").asText("");
        if (outputText.isBlank()) throw new IllegalArgumentException("Chat Completions APIの本文が空です");
        return parseSummaryJson(stripCodeFence(outputText));
    }

    private String stripCodeFence(String text) {
        String value = text.trim();
        if (!value.startsWith("```")) return value;
        int firstLine = value.indexOf('\n');
        int closing = value.lastIndexOf("```");
        if (firstLine < 0 || closing <= firstLine) return value;
        return value.substring(firstLine + 1, closing).trim();
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
