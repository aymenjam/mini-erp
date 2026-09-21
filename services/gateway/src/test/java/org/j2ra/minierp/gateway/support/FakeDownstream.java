package org.j2ra.minierp.gateway.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * A fake "customers" for the gateway tests: a JDK HttpServer that records what it gets and answers what the test says.
 * One instance for the whole test run, so Spring can reuse the same application context. Call reset() before each test.
 */
public final class FakeDownstream {

    public static final FakeDownstream INSTANCE = new FakeDownstream();

    /** One request as the downstream saw it. Header names are case-insensitive. */
    public record Received(String method, String path, Map<String, List<String>> headers) {
        public String header(String name) {
            List<String> values = headers.get(name);
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }

    private record Answer(int status, String contentType, String body, Duration delay, Map<String, String> headers) {
    }

    private final HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private volatile Answer answer;

    private FakeDownstream() {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            // Several threads: a slow answer (timeout test) must not block the next test.
            server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "fake-downstream");
                thread.setDaemon(true);
                return thread;
            }));
            server.createContext("/", this::handle);
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        reset();
    }

    public URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    /** Forget the requests and go back to the default answer: 200 with an empty customer page. */
    public void reset() {
        received.clear();
        answer = new Answer(200, "application/json", "{\"content\":[]}", Duration.ZERO, Map.of());
    }

    public void respond(int status, String contentType, String body) {
        answer = new Answer(status, contentType, body, Duration.ZERO, Map.of());
    }

    public void respondWithHeader(String headerName, String headerValue) {
        Answer current = answer;
        answer = new Answer(current.status, current.contentType, current.body, current.delay, Map.of(headerName, headerValue));
    }

    public void respondAfter(Duration delay) {
        Answer current = answer;
        answer = new Answer(current.status, current.contentType, current.body, delay, current.headers);
    }

    public List<Received> requests() {
        return List.copyOf(received);
    }

    public Received lastRequest() {
        List<Received> all = requests();
        if (all.isEmpty()) {
            throw new AssertionError("The downstream got no request");
        }
        return all.get(all.size() - 1);
    }

    private void handle(HttpExchange exchange) throws IOException {
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(exchange.getRequestHeaders());
        received.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), headers));

        Answer current = answer;
        try {
            if (!current.delay.isZero()) {
                Thread.sleep(current.delay.toMillis());
            }
            byte[] body = current.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", current.contentType);
            new LinkedHashMap<>(current.headers).forEach((name, value) -> exchange.getResponseHeaders().set(name, value));
            exchange.sendResponseHeaders(current.status, body.length);
            exchange.getResponseBody().write(body);
        } catch (IOException | InterruptedException e) {
            // The gateway may have given up already (timeout test). Nothing to do.
        } finally {
            exchange.close();
        }
    }
}
