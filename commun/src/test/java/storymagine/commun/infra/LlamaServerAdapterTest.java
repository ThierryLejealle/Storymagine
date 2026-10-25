package storymagine.commun.infra;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import storymagine.commun.coeur.ports.ContextOverflowException;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.LogPort;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie le parsing llama-server (sync /v1/chat/completions, streaming SSE, brut /completion)
 * avec un faux serveur HTTP local — jamais de vrai LLM (voir CLAUDE.md). Couvre spécifiquement ce
 * qui est propre à ce nouvel adaptateur : format OpenAI-compatible, extraction
 * reasoning_content/timings, et l'absence de catalogue (listModels toujours vide).
 */
class LlamaServerAdapterTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    private LlamaServerConfig configFor(String baseUrl, boolean streamMode) {
        return new LlamaServerConfig(baseUrl, 4096, 40, 0.9, 1.1, -1,
            streamMode, 5000, 5000, 5000, new RetryPolicy(0, 0, 0, 0));
    }

    @Test
    void generateSyncParsesTextAndReasoning() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"bonjour\","
                + "\"reasoning_content\":\"reflexion\"}}],"
                + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":3},"
                + "\"timings\":{\"predicted_ms\":150.0,\"predicted_n\":3,\"prompt_n\":10}}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();

        LlamaServerAdapter llm = configFor(baseUrl(), false).adapter("test-model", false, LogPort.NOOP);

        LlmResult result = llm.generate("system", "user", 0.7, LlmCallContext.of("test"));

        // Non-regression 2026-09-14 : llama-server rejetait ("type must be string, but is null")
        // les messages de requete a cause d'un "reasoning_content":null serialise a tort — ce
        // champ ne doit jamais apparaitre dans une requete (voir LlamaServerMessage).
        assertTrue(requestBody.get() != null && !requestBody.get().contains("reasoning_content"),
            "la requete ne doit jamais contenir reasoning_content : " + requestBody.get());

        assertEquals("bonjour", result.text());
        assertEquals("reflexion", result.thinking());
        assertEquals(10, result.promptTokens());
        assertEquals(3, result.responseTokens());
        assertTrue(result.evalDurationNs() > 0);
    }

    @Test
    void generateStreamingConcatenatesDeltasAndReasoning() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(sse("{\"choices\":[{\"delta\":{\"reasoning_content\":\"think\"}}]}"));
                os.write(sse("{\"choices\":[{\"delta\":{\"reasoning_content\":\"ing\"}}]}"));
                os.write(sse("{\"choices\":[{\"delta\":{\"content\":\"bon\"}}]}"));
                os.write(sse("{\"choices\":[{\"delta\":{\"content\":\"jour\"}}],"
                    + "\"timings\":{\"predicted_ms\":80.0,\"predicted_n\":2,\"prompt_n\":5}}"));
                os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });
        server.start();

        LlamaServerAdapter llm = configFor(baseUrl(), true).adapter("test-model", false, LogPort.NOOP);

        AtomicReference<String> lastPartial = new AtomicReference<>("");
        LlmResult result = llm.generate("system", "user", 0.7, LlmCallContext.of("test"),
            GenerationOptions.NONE, partial -> lastPartial.set(partial.textSoFar()));

        assertEquals("bonjour", result.text());
        assertEquals("thinking", result.thinking());
        assertEquals("bonjour", lastPartial.get());
        assertEquals(2, result.responseTokens());
    }

    @Test
    void completeRawParsesCompletionEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/completion", exchange -> {
            String json = "{\"content\":\"suite du texte\",\"tokens_predicted\":4,\"tokens_evaluated\":12}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();

        LlamaServerAdapter llm = configFor(baseUrl(), false).adapter("test-model", false, LogPort.NOOP);

        LlmResult result = llm.complete("continue ce texte", 0.5, LlmCallContext.of("test"));

        assertEquals("suite du texte", result.text());
        assertEquals(12, result.promptTokens());
        assertEquals(4, result.responseTokens());
    }

    @Test
    void httpErrorMentioningContextOverflowThrowsContextOverflowException() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = "the request exceeds the available context size".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
        });
        server.start();

        LlamaServerAdapter llm = configFor(baseUrl(), false).adapter("test-model", false, LogPort.NOOP);

        assertThrows(ContextOverflowException.class,
            () -> llm.generate("system", "user", 0.7, LlmCallContext.of("test")));
    }

    @Test
    void listModelsIsAlwaysEmpty() {
        LlamaServerAdapter llm = configFor("http://127.0.0.1:1", false).adapter("test-model");
        assertTrue(llm.listModels(Long.MAX_VALUE).isEmpty());
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static byte[] sse(String json) {
        return ("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8);
    }
}
