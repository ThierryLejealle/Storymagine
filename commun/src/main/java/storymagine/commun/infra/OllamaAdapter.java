package storymagine.commun.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import storymagine.commun.coeur.ports.ContextOverflowException;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.coeur.ports.ModelLifecyclePort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptateur Ollama — ModelCallPort et ModelLifecyclePort.
 * Mode sync : timeout global sur la requête HTTP.
 * Mode stream : timeout premier token (chargement + prefill) + timeout inter-token.
 */
public class OllamaAdapter implements ModelCallPort, ModelLifecyclePort {

    private static final ObjectMapper    JSON                = new ObjectMapper();
    private static final double          CONTEXT_GROW_FACTOR = 1.3;
    private static final ExecutorService STREAM_EXECUTOR     = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ollama-stream");
        t.setDaemon(true);
        return t;
    });

    private final String      baseUrl;
    private final String      model;
    private final boolean     think;
    private final boolean     streamMode;
    private final RetryPolicy retryPolicy;
    private final int         maxContextWindowSize;
    private final int         topK;
    private final double      topP;
    private final double      repeatPenalty;
    private final int         numPredict;
    private final int         timeoutMs;           // sync : timeout total requête
    private final int         firstTokenTimeoutMs; // stream : chargement modèle + prefill → 1er token
    private final int         interTokenTimeoutMs; // stream : délai max entre deux tokens
    private final HttpClient  http;
    private final LogPort     log;

    private int             contextWindowSize;
    private OllamaModelInfo cachedModelInfo = null;

    private final Map<String, AtomicInteger> agentCallCounts = new ConcurrentHashMap<>();

    OllamaAdapter(String baseUrl, String model, int contextWindowSize, int maxContextWindowSize,
                  int topK, double topP, double repeatPenalty, int numPredict,
                  boolean streamMode, int timeoutMs, int firstTokenTimeoutMs, int interTokenTimeoutMs,
                  boolean think, RetryPolicy retryPolicy, LogPort log) {
        this.baseUrl              = baseUrl;
        this.model                = model;
        this.think                = think;
        this.streamMode           = streamMode;
        this.retryPolicy          = retryPolicy;
        this.contextWindowSize    = contextWindowSize;
        this.maxContextWindowSize = maxContextWindowSize;
        this.topK                 = topK;
        this.topP                 = topP;
        this.repeatPenalty        = repeatPenalty;
        this.numPredict           = numPredict;
        this.timeoutMs            = timeoutMs;
        this.firstTokenTimeoutMs  = firstTokenTimeoutMs;
        this.interTokenTimeoutMs  = interTokenTimeoutMs;
        this.log                  = log;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    // -------------------------------------------------------------------------
    // ModelCallPort
    // -------------------------------------------------------------------------

    @Override
    public LlmResult generate(String systemPrompt, String userPrompt, double temperature,
                               LlmCallContext ctx) {
        int    local  = agentCallCounts
                            .computeIfAbsent(ctx.agentName(), k -> new AtomicInteger(0))
                            .incrementAndGet();
        long   t0     = System.currentTimeMillis();
        String handle = log.llmCallOpen(ctx.agentName(), local, systemPrompt, userPrompt);
        try {
            LlmResult result = generateInternal(systemPrompt, userPrompt, temperature, ctx.agentLabel());
            log.llmCallClose(handle, result.text(), System.currentTimeMillis() - t0,
                    result.promptTokens(), result.responseTokens());
            return result;
        } catch (RuntimeException e) {
            log.llmCallClose(handle, "ERREUR : " + e.getMessage(),
                    System.currentTimeMillis() - t0, 0, 0);
            throw e;
        }
    }

    private LlmResult generateInternal(String systemPrompt, String userPrompt, double temperature,
                                        String agentLabel) {
        while (true) {
            try {
                return streamMode
                    ? sendStreaming(systemPrompt, userPrompt, temperature, agentLabel)
                    : sendSync(systemPrompt, userPrompt, temperature, agentLabel);
            } catch (ContextOverflowException e) {
                int newSize = Math.min((int) (contextWindowSize * CONTEXT_GROW_FACTOR), maxContextWindowSize);
                if (newSize <= contextWindowSize) {
                    throw new RuntimeException(
                        "Contexte Ollama saturé au maximum (" + maxContextWindowSize + " tokens) : " + e.getMessage(), e);
                }
                System.out.printf("[Ollama] Dépassement contexte — extension %d → %d tokens, reprise…%n",
                    contextWindowSize, newSize);
                contextWindowSize = newSize;
            }
        }
    }

    // -------------------------------------------------------------------------
    // ModelLifecyclePort
    // -------------------------------------------------------------------------

    @Override
    public void probe() {
        OllamaModelInfo info = fetchModelInfo();
        generate("", "test", 0.0);
        fetchLoadedInfo(info);
        cachedModelInfo = info;
    }

    @Override
    public void unload() {
        try {
            String body = "{\"model\":\"" + model + "\",\"stream\":false,\"keep_alive\":0}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            if (resp.statusCode() != 200)
                System.err.printf("[Ollama] unload(%s) HTTP %d%n", model, resp.statusCode());
        } catch (Exception e) {
            System.err.printf("[Ollama] unload(%s) ignoré : %s%n", model, e.getMessage());
        }
    }

    @Override public String  modelName()         { return model; }
    @Override public int     contextWindow()      { return contextWindowSize; }
    @Override public String  modelParamSize()     { return cachedModelInfo != null ? cachedModelInfo.parameterSize : ""; }
    @Override public String  modelQuantization()  { return cachedModelInfo != null ? cachedModelInfo.quantization  : ""; }
    @Override public String  modelFamily()        { return cachedModelInfo != null ? cachedModelInfo.family        : ""; }
    @Override public boolean supportsThinking()   { return cachedModelInfo != null && cachedModelInfo.supportsThinking; }

    @Override
    public String modelInfoBlock() {
        if (cachedModelInfo == null) return "";
        String declared = cachedModelInfo.formatDeclared(model, contextWindowSize);
        String runtime  = cachedModelInfo.formatRuntime();
        return runtime.isEmpty() ? declared : declared + "\n" + runtime;
    }

    @Override
    public List<ModelEntry> listModels(long maxBytes) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new RuntimeException("Ollama /api/tags → HTTP " + response.statusCode());

            JsonNode models = JSON.readTree(response.body()).path("models");
            List<ModelEntry> entries = new ArrayList<>();
            if (models.isArray()) {
                for (JsonNode m : models) {
                    String name = m.path("name").asText("");
                    if (name.isBlank()) continue;
                    long size = m.path("size").asLong(0);
                    if (maxBytes > 0 && size > maxBytes) {
                        System.out.printf("[Ollama] ignoré (%.1f Go > %.1f Go max) : %s%n",
                            size / 1_073_741_824.0, maxBytes / 1_073_741_824.0, name);
                        continue;
                    }
                    entries.add(new ModelEntry(name, size));
                }
            }
            entries.sort(java.util.Comparator.comparingLong(ModelEntry::sizeBytes));
            return entries;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrompu pendant la liste des modèles Ollama", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Liste des modèles Ollama impossible : " + e.getMessage(), e);
        }
    }

    @Override
    public int detectContext(String model) {
        try {
            String body = "{\"model\":\"" + model.replace("\"", "\\\"") + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/show"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode modelInfo = JSON.readTree(resp.body()).path("model_info");
            for (var entry : modelInfo.properties()) {
                if (entry.getKey().endsWith(".context_length"))
                    return entry.getValue().asInt(32768);
            }
        } catch (Exception ignored) {}
        return 32768;
    }

    // -------------------------------------------------------------------------
    // Appel LLM — mode sync (timeout global sur la requête)
    // -------------------------------------------------------------------------

    private LlmResult sendSync(String systemPrompt, String userPrompt, double temperature,
                                String agentLabel) {
        OllamaRequest req = buildOllamaRequest(systemPrompt, userPrompt, temperature);

        HttpRequest request;
        try {
            String body = JSON.writeValueAsString(req);
            request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Construction requête Ollama échouée : " + e.getMessage(), e);
        }

        int       totalAttempts = retryPolicy.retryCount() + 1;
        Exception lastCause     = null;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    String errorBody = response.body();
                    if (isContextOverflow(errorBody))
                        throw new ContextOverflowException(errorBody, contextWindowSize);
                    throw new RuntimeException("Ollama HTTP " + response.statusCode() + " : " + errorBody);
                }
                OllamaResponse resp = JSON.readValue(response.body(), OllamaResponse.class);
                if (resp.error != null && !resp.error.isBlank()) {
                    if (isContextOverflow(resp.error))
                        throw new ContextOverflowException(resp.error, contextWindowSize);
                    throw new RuntimeException("Ollama error : " + resp.error);
                }
                String text = resp.message != null ? resp.message.content : "";
                logLlmCall(agentLabel, resp.promptEvalCount, resp.evalCount, resp.evalDuration);
                return new LlmResult(text, resp.promptEvalCount, resp.evalCount, resp.evalDuration);

            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrompu pendant un appel Ollama", e);
            } catch (Exception e) {
                lastCause = e;
                if (attempt < totalAttempts) {
                    int waitSec = retryPolicy.delaySeconds(attempt);
                    System.err.printf("[Ollama] tentative %d/%d échouée (%s), reprise dans %ds…%n",
                        attempt, totalAttempts, e.getMessage(), waitSec);
                    try { Thread.sleep(waitSec * 1000L); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrompu pendant l'attente de reprise", ie);
                    }
                }
            }
        }
        throw new RuntimeException(
            "Ollama : échec après " + totalAttempts + " tentative(s) : " + lastCause.getMessage(), lastCause);
    }

    // -------------------------------------------------------------------------
    // Appel LLM — mode stream (timeout premier token + timeout inter-token)
    // -------------------------------------------------------------------------

    private LlmResult sendStreaming(String systemPrompt, String userPrompt, double temperature,
                                     String agentLabel) {
        OllamaRequest req = buildOllamaRequest(systemPrompt, userPrompt, temperature);
        req.stream = true;

        HttpRequest request;
        try {
            String body = JSON.writeValueAsString(req);
            request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Construction requête Ollama (stream) échouée : " + e.getMessage(), e);
        }

        int       totalAttempts = retryPolicy.retryCount() + 1;
        Exception lastCause     = null;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                return executeStreaming(request, agentLabel);
            } catch (ContextOverflowException e) {
                throw e;
            } catch (Exception e) {
                lastCause = e;
                if (attempt < totalAttempts) {
                    int waitSec = retryPolicy.delaySeconds(attempt);
                    System.err.printf("[Ollama] tentative stream %d/%d échouée (%s), reprise dans %ds…%n",
                        attempt, totalAttempts, e.getMessage(), waitSec);
                    try { Thread.sleep(waitSec * 1000L); }
                    catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrompu pendant l'attente de reprise", ie);
                    }
                }
            }
        }
        throw new RuntimeException(
            "Ollama stream : échec après " + totalAttempts + " tentative(s) : " + lastCause.getMessage(), lastCause);
    }

    private LlmResult executeStreaming(HttpRequest request, String agentLabel) {
        // Obtenir les headers HTTP — quasi-instantané sur Ollama local
        CompletableFuture<HttpResponse<InputStream>> cf =
            http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

        HttpResponse<InputStream> response;
        try {
            response = cf.get(30_000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cf.cancel(true);
            throw new RuntimeException("Timeout connexion Ollama (30s)");
        } catch (ExecutionException e) {
            throw new RuntimeException("Erreur connexion Ollama : " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrompu en attente de connexion Ollama");
        }

        if (response.statusCode() != 200) {
            try (InputStream is = response.body()) {
                String errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (isContextOverflow(errorBody))
                    throw new ContextOverflowException(errorBody, contextWindowSize);
                throw new RuntimeException("Ollama HTTP " + response.statusCode() + " : " + errorBody);
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new RuntimeException("Ollama HTTP " + response.statusCode());
            }
        }

        StringBuilder content      = new StringBuilder();
        int           promptTokens = 0;
        int           evalTokens   = 0;
        long          evalDuration = 0;

        try (InputStream is = response.body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            boolean firstToken = true;
            while (true) {
                long   timeout = firstToken ? firstTokenTimeoutMs : interTokenTimeoutMs;
                String line    = readLineWithTimeout(reader, is, timeout);
                if (line == null) break;
                if (line.isBlank()) continue;
                firstToken = false;

                OllamaResponse chunk = JSON.readValue(line, OllamaResponse.class);
                if (chunk.error != null && !chunk.error.isBlank()) {
                    if (isContextOverflow(chunk.error))
                        throw new ContextOverflowException(chunk.error, contextWindowSize);
                    throw new RuntimeException("Ollama streaming error : " + chunk.error);
                }
                // Accumule content uniquement — le champ thinking est intentionnellement ignoré
                if (chunk.message != null && chunk.message.content != null) {
                    content.append(chunk.message.content);
                }
                if (chunk.done) {
                    promptTokens = chunk.promptEvalCount;
                    evalTokens   = chunk.evalCount;
                    evalDuration = chunk.evalDuration;
                    break;
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture stream Ollama : " + e.getMessage(), e);
        }

        logLlmCall(agentLabel, promptTokens, evalTokens, evalDuration);
        return new LlmResult(content.toString(), promptTokens, evalTokens, evalDuration);
    }

    /**
     * Lit une ligne avec timeout — ferme le flux sur expiration pour débloquer le thread lecteur.
     */
    private static String readLineWithTimeout(BufferedReader reader, InputStream raw, long timeoutMs) {
        Future<String> future = STREAM_EXECUTOR.submit(reader::readLine);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            try { raw.close(); } catch (IOException ignored) {}
            throw new RuntimeException("Timeout stream Ollama (" + timeoutMs + "ms sans nouveau token)");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Erreur lecture stream : " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrompu pendant la lecture stream Ollama");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers communs
    // -------------------------------------------------------------------------

    private OllamaRequest buildOllamaRequest(String systemPrompt, String userPrompt, double temperature) {
        OllamaRequest req = new OllamaRequest();
        req.model = model;
        req.options.put("num_ctx",        contextWindowSize);
        req.options.put("temperature",    temperature);
        req.options.put("top_k",          topK);
        req.options.put("top_p",          topP);
        req.options.put("repeat_penalty", repeatPenalty);
        if (numPredict >= 0) req.options.put("num_predict", numPredict);
        req.think    = think ? Boolean.TRUE : null;
        req.messages = List.of(
            new OllamaMessage("system", systemPrompt),
            new OllamaMessage("user",   userPrompt)
        );
        return req;
    }

    private void logLlmCall(String agentLabel, int promptTokens, int evalTokens, long evalDurationNs) {
        long   ms  = evalDurationNs > 0 ? evalDurationNs / 1_000_000L : 0;
        double tps = evalTokens > 0 && evalDurationNs > 0
            ? evalTokens / (evalDurationNs / 1_000_000_000.0) : 0;
        log.llmCall(agentLabel, ms, promptTokens, evalTokens, tps);
    }

    private OllamaModelInfo fetchModelInfo() {
        OllamaModelInfo info = new OllamaModelInfo();
        try {
            String body = "{\"name\":\"" + model + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/show"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return info;

            JsonNode root    = JSON.readTree(resp.body());
            JsonNode details = root.path("details");
            info.parameterSize = details.path("parameter_size").asText("");
            info.quantization  = details.path("quantization_level").asText("");
            info.family        = details.path("family").asText("");

            JsonNode modelInfo = root.path("model_info");
            info.nativeCtxLength = findLong(modelInfo, "context_length");
            info.embeddingLength = (int) findLong(modelInfo, "embedding_length");
            info.layerCount      = (int) findLong(modelInfo, "block_count", "layer_count");
            info.headCount       = (int) findLong(modelInfo, "attention.head_count");

            JsonNode caps = root.path("capabilities");
            if (caps.isArray()) {
                for (JsonNode cap : caps) {
                    if ("thinking".equals(cap.asText())) { info.supportsThinking = true; break; }
                }
            }
        } catch (Exception e) {
            System.err.printf("[Ollama] /api/show ignoré : %s%n", e.getMessage());
        }
        return info;
    }

    private void fetchLoadedInfo(OllamaModelInfo info) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/ps"))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;

            JsonNode models = JSON.readTree(resp.body()).path("models");
            if (!models.isArray()) return;
            for (JsonNode m : models) {
                String name = m.path("name").asText("");
                if (name.startsWith(model) || model.startsWith(name.split(":")[0])) {
                    info.diskBytes = m.path("size").asLong(0);
                    info.vramBytes = m.path("size_vram").asLong(0);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.printf("[Ollama] /api/ps ignoré : %s%n", e.getMessage());
        }
    }

    private static long findLong(JsonNode node, String... suffixes) {
        for (String suffix : suffixes) {
            var it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                if (key.equals(suffix) || key.endsWith("." + suffix))
                    return node.get(key).asLong(0);
            }
        }
        return 0;
    }

    private static boolean isContextOverflow(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("num_ctx")
            || lower.contains("truncat")
            || lower.contains("too large")
            || (lower.contains("context") && (lower.contains("exceed") || lower.contains("limit") || lower.contains("overflow")));
    }
}
