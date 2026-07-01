package com.example.techwatch.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SafeHttpClient {
    private final HttpClient client;
    private final UrlSafetyPolicy policy;
    private final int maximumBytes;
    private final int maximumRedirects;

    public SafeHttpClient(HttpClient client, UrlSafetyPolicy policy, int maximumBytes, int maximumRedirects) {
        if (client.followRedirects() != HttpClient.Redirect.NEVER) {
            throw new IllegalArgumentException("SafeHttpClientには自動リダイレクト無効のHttpClientが必要です");
        }
        this.client = client;
        this.policy = policy;
        this.maximumBytes = maximumBytes;
        this.maximumRedirects = maximumRedirects;
    }

    public Response get(URI initial, String accept, Duration timeout) throws IOException, InterruptedException {
        URI current = initial;
        for (int redirects = 0; ; redirects++) {
            policy.validate(current);
            HttpRequest request = HttpRequest.newBuilder(current).timeout(timeout)
                    .header("User-Agent", "tekkunews/1.4 (+local research tool)")
                    .header("Accept", accept).GET().build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (isRedirect(response.statusCode())) {
                    if (redirects >= maximumRedirects) throw new IOException("リダイレクト回数が上限を超えました");
                    String location = response.headers().firstValue("Location")
                            .orElseThrow(() -> new IOException("リダイレクト先がありません"));
                    current = current.resolve(location);
                    continue;
                }
                byte[] bytes = body.readNBytes(maximumBytes + 1);
                if (bytes.length > maximumBytes) throw new IOException("レスポンスサイズが上限を超えました");
                return new Response(response.statusCode(), bytes, current);
            }
        }
    }

    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    public record Response(int statusCode, byte[] body, URI finalUri) { }
}
