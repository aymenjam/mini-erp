package org.j2ra.minierp.chaintest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small HTTP client for the tests (JDK only). It does not follow redirects, so a test can look at them. */
final class Http {

    record Response(int status, HttpHeaders headers, String body) {

        String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        List<String> headerValues(String name) {
            return headers.allValues(name);
        }

        String contentType() {
            String value = header("Content-Type");
            return value == null ? "" : value.toLowerCase();
        }

        /** Names of all headers of this answer, in lower case. */
        List<String> headerNames() {
            return headers.map().keySet().stream().map(String::toLowerCase).toList();
        }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private Http() {
    }

    /** @param headerPairs name, value, name, value, ... */
    static Response get(String url, String... headerPairs) {
        return send("GET", url, headerPairs);
    }

    static Response options(String url, String... headerPairs) {
        return send("OPTIONS", url, headerPairs);
    }

    static Response send(String method, String url, String... headerPairs) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .method(method, HttpRequest.BodyPublishers.noBody());
        for (Map.Entry<String, String> header : pairs(headerPairs).entrySet()) {
            request.header(header.getKey(), header.getValue());
        }
        return execute(request.build());
    }

    static Response postForm(String url, Map<String, String> form) {
        StringBuilder body = new StringBuilder();
        form.forEach((name, value) -> {
            if (!body.isEmpty()) {
                body.append('&');
            }
            body.append(java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)).append('=')
                    .append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
        });
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
        return execute(request);
    }

    private static Response execute(HttpRequest request) {
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.headers(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(request.method() + " " + request.uri() + " failed: " + e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(request.method() + " " + request.uri() + " was interrupted", e);
        }
    }

    private static Map<String, String> pairs(String... headerPairs) {
        if (headerPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Headers must be given as name, value pairs");
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 0; i < headerPairs.length; i += 2) {
            headers.put(headerPairs[i], headerPairs[i + 1]);
        }
        return headers;
    }
}
