package storymagine.commun.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import storymagine.commun.coeur.ports.ContextOverflowException;
import storymagine.commun.coeur.ports.GenerationCancelledException;
import storymagine.commun.coeur.ports.GenerationOptions;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.coeur.ports.ModelLifecyclePort;
import storymagine.commun.coeur.ports.PartialGeneration;
import storymagine.commun.coeur.ports.RawCompletionPort;

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
import java.util.function.Consumer;

/**
 * Adaptateur llama-server (llama.cpp) — ModelCallPort, ModelLifecyclePort et RawCompletionPort.
 * Différences structurelles avec OllamaAdapter (décision projet du 2026-09-14) :
 * <ul>
 *   <li>un seul modèle déjà chargé par process, pas de catalogue interrogeable — listModels()
 *       renvoie toujours une liste vide, le cœur/l'UI en déduisent qu'aucun choix de modèle n'est
 *       possible ici ;</li>
 *   <li>la fenêtre de contexte est fixée au lancement du process llama-server (-c) : pas de
 *       redimensionnement à la volée comme le num_ctx d'Ollama — un dépassement de contexte
 *       remonte directement en erreur, sans tentative d'agrandissement ;</li>
 *   <li>le programme ne démarre ni n'arrête jamais le process llama-server (pas d'équivalent de
 *       OllamaLauncher) : unload() est un no-op, et probe() se contente d'interroger un serveur
 *       déjà en marche.</li>
 * </ul>
 * Mode sync : timeout global sur la requête HTTP. Mode stream : timeout premier token
 * (chargement + prefill) + timeout inter-token — même convention que OllamaAdapter.
 */
public class LlamaServerAdapter implements ModelCallPort, ModelLifecyclePort, RawCompletionPort {

    private static final ObjectMapper    JSON                = new ObjectMapper();
    private static final ExecutorService STREAM_EXECUTOR     = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "llamaserver-stream");
        t.setDaemon(true);
        return t;
    });

    private final String      baseUrl;
    private final String      model;
    private final boolean     think;
    private final boolean     streamMode;
    private final RetryPolicy retryPolicy;
    private final int         contextWindowSize;
    private final int         topK;
    private final double      topP;
    private final double      repeatPenalty;
    private final int         numPredict;
    private final int         timeoutMs;           // sync : timeout total requête
    private final int         firstTokenTimeoutMs; // stream : chargement modèle + prefill → 1er token
    private final int         interTokenTimeoutMs; // stream : délai max entre deux tokens
    private final HttpClient  http;
    private final LogPort     log;

    private String cachedModelPath = "";

    private final Map<String, AtomicInteger> agentCallCounts = new ConcurrentHashMap<>();

    LlamaServerAdapter(String baseUrl, String model, int contextWindowSize,
                        int topK, double topP, double repeatPenalty, int numPredict,
                        boolean streamMode, int timeoutMs, int firstTokenTimeoutMs, int interTokenTimeoutMs,
                        boolean think, RetryPolicy retryPolicy, LogPort log) {
        this.baseUrl              = baseUrl;
        this.model                = model;
        this.think                = think;
        this.streamMode           = streamMode;
        this.retryPolicy          = retryPolicy;
        this.contextWindowSize    = contextWindowSize;
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
    // ModelCallPort — /v1/chat/completions, format OpenAI-compatible
    // -------------------------------------------------------------------------

    @Override
    public LlmResult generate(String systemPrompt, String userPrompt, double temperature,
                               LlmCallContext ctx) {
        return generate(systemPrompt, userPrompt, temperature, ctx, GenerationOptions.NONE);
    }

    @Override
    public LlmResult generate(String systemPrompt, String userPrompt, double temperature,
                               LlmCallContext ctx, GenerationOptions options) {
        return generate(systemPrompt, userPrompt, temperature, ctx, options, partial -> {});
    }

    @Override
    public LlmResult generate(String systemPrompt, String userPrompt, double temperature,
                               LlmCallContext ctx, GenerationOptions options, Consumer<PartialGeneration> onPartial) {
        warnIfContextOverrideIgnored(options);
        int    local  = agentCallCounts
                            .computeIfAbsent(ctx.agentName(), k -> new AtomicInteger(0))
                            .incrementAndGet();
        long   t0     = System.currentTimeMillis();
        Boolean wanted = wantedThink(ctx.think());
        String handle = log.llmCallOpen(ctx.agentName(), local, systemPrompt, userPrompt, wanted);
        try {
            LlmResult result = streamMode
                ? sendStreaming(systemPrompt, userPrompt, temperature, ctx.agentLabel(), options, onPartial)
                : sendSync(systemPrompt, userPrompt, temperature, ctx.agentLabel(), options);
            log.llmCallClose(handle, result.text(), result.thinking(), System.currentTimeMillis() - t0,
                    result.promptTokens(), result.responseTokens());
            return result;
        } catch (RuntimeException e) {
            log.llmCallClose(handle, "ERREUR : " + e.getMessage(), "",
                    System.currentTimeMillis() - t0, 0, 0);
            throw e;
        }
    }

    /**
     * llama-server fixe sa fenêtre de contexte au lancement du process (-c) : contrairement à
     * Ollama, ce programme ne peut ni l'agrandir à la volée ni redémarrer le process pour en
     * changer (voir LlamaServerConfig). Un override demandé ici (ex. réglage de contexte en
     * session côté chat) est donc simplement ignoré, avec un avertissement plutôt qu'un échec
     * silencieux.
     */
    private void warnIfContextOverrideIgnored(GenerationOptions options) {
        if (options.contextWindow() != null && options.contextWindow() != contextWindowSize) {
            log.warn("[llama-server] fenêtre de contexte demandée (" + options.contextWindow()
                + ") ignorée — fixée au lancement du process llama-server (-c=" + contextWindowSize
                + "), non modifiable à chaud.");
        }
    }

    // -------------------------------------------------------------------------
    // RawCompletionPort — /completion, un seul bloc de texte, sans rôles ni template
    // -------------------------------------------------------------------------

    @Override
    public LlmResult complete(String prompt, double temperature, LlmCallContext ctx) {
        int    local  = agentCallCounts
                            .computeIfAbsent(ctx.agentName(), k -> new AtomicInteger(0))
                            .incrementAndGet();
        long   t0     = System.currentTimeMillis();
        String handle = log.llmCallOpen(ctx.agentName(), local, "", prompt, wantedThink(ctx.think()));
        try {
            LlmResult result = sendRawSync(prompt, temperature);
            log.llmCallClose(handle, result.text(), result.thinking(), System.currentTimeMillis() - t0,
                    result.promptTokens(), result.responseTokens());
            return result;
        } catch (RuntimeException e) {
            log.llmCallClose(handle, "ERREUR : " + e.getMessage(), "",
                    System.currentTimeMillis() - t0, 0, 0);
            throw e;
        }
    }

    private LlmResult sendRawSync(String prompt, double temperature) {
        LlamaServerCompletionRequest req = new LlamaServerCompletionRequest();
        req.prompt        = prompt;
        req.temperature   = temperature;
        req.topK          = topK;
        req.topP          = topP;
        req.repeatPenalty = repeatPenalty;
        if (numPredict >= 0) req.nPredict = numPredict;

        HttpRequest request;
        try {
            String body = JSON.writeValueAsString(req);
            request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/completion"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Construction requête llama-server (completion) échouée : " + e.getMessage(), e);
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
                    throw new RuntimeException("llama-server HTTP " + response.statusCode() + " : " + errorBody);
                }
                LlamaServerCompletionResponse resp = JSON.readValue(response.body(), LlamaServerCompletionResponse.class);
                if (resp.error != null && !resp.error.isBlank()) {
                    if (isContextOverflow(resp.error))
                        throw new ContextOverflowException(resp.error, contextWindowSize);
                    throw new RuntimeException("llama-server error : " + resp.error);
                }
                String text = resp.content != null ? resp.content : "";
                logLlmCall("chat/raw", resp.tokensEvaluated, resp.tokensPredicted, 0, wantedThink(null));
                return new LlmResult(text, resp.tokensEvaluated, resp.tokensPredicted, 0, "");

            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrompu pendant un appel llama-server", e);
            } catch (Exception e) {
                lastCause = e;
                if (attempt < totalAttempts) {
                    int waitSec = retryPolicy.delaySeconds(attempt);
                    System.err.printf("[llama-server] tentative %d/%d échouée (%s), reprise dans %ds…%n",
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
            "llama-server : échec après " + totalAttempts + " tentative(s) : " + lastCause.getMessage(), lastCause);
    }

    // -------------------------------------------------------------------------
    // ModelLifecyclePort
    // -------------------------------------------------------------------------

    @Override
    public void probe() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200)
                throw new RuntimeException("llama-server /health → HTTP " + resp.statusCode());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("llama-server injoignable (" + baseUrl + ") : " + e.getMessage(), e);
        }
        fetchModelInfo();
        generate("", "test", 0.0);
    }

    /**
     * llama-server ne sert qu'un seul modèle par process — le décharger nécessiterait de tuer le
     * process, ce que ce programme ne fait jamais (voir décision projet 2026-09-14). No-op.
     */
    @Override
    public void unload() {
    }

    @Override public String  modelName()         { return model; }
    @Override public int     contextWindow()      { return contextWindowSize; }
    /** llama-server n'expose pas ces informations via son API standard — toujours vide. */
    @Override public String  modelParamSize()     { return ""; }
    /** llama-server n'expose pas ces informations via son API standard — toujours vide. */
    @Override public String  modelQuantization()  { return ""; }

    @Override
    public String modelFamily() {
        if (cachedModelPath.isBlank()) return "";
        String normalized = cachedModelPath.replace('\\', '/');
        int    slash      = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    @Override
    public boolean supportsThinking() {
        return think;
    }

    @Override
    public String modelInfoBlock() {
        String family = modelFamily();
        return "Modele : " + model + " (llama-server, ctx=" + contextWindowSize + " tokens)"
            + (family.isBlank() ? "" : " — " + family);
    }

    /**
     * llama-server ne sert qu'un seul modèle déjà chargé au lancement du process : pas de
     * catalogue interrogeable (pas d'équivalent de /api/tags). Liste vide : le cœur/l'UI en
     * déduisent qu'aucun choix de modèle n'est possible ici — voir décision projet 2026-09-14.
     */
    @Override
    public List<ModelEntry> listModels(long maxBytes) {
        return List.of();
    }

    /** Seule valeur connue : la fenêtre configurée au lancement de ce process. */
    @Override
    public int detectContext(String model) {
        return contextWindowSize;
    }

    // -------------------------------------------------------------------------
    // Appel LLM — mode sync (timeout global sur la requête)
    // -------------------------------------------------------------------------

    private LlmResult sendSync(String systemPrompt, String userPrompt, double temperature,
                                String agentLabel, GenerationOptions options) {
        LlamaServerChatRequest req = buildChatRequest(systemPrompt, userPrompt, temperature, options);

        HttpRequest request;
        try {
            String body = JSON.writeValueAsString(req);
            request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(timeoutMs))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Construction requête llama-server échouée : " + e.getMessage(), e);
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
                    throw new RuntimeException("llama-server HTTP " + response.statusCode() + " : " + errorBody);
                }
                LlamaServerChatResponse resp = JSON.readValue(response.body(), LlamaServerChatResponse.class);
                if (resp.error != null && !resp.error.isBlank()) {
                    if (isContextOverflow(resp.error))
                        throw new ContextOverflowException(resp.error, contextWindowSize);
                    throw new RuntimeException("llama-server error : " + resp.error);
                }
                LlamaServerMessage message        = firstMessage(resp);
                String             text           = message != null && message.content != null ? message.content : "";
                String             thinking       = message != null && message.reasoningContent != null
                                                        ? message.reasoningContent : "";
                int                promptTokens   = promptTokens(resp);
                int                responseTokens = responseTokens(resp);
                long               evalDurationNs = evalDurationNs(resp);
                logLlmCall(agentLabel, promptTokens, responseTokens, evalDurationNs, wantedThink(null));
                return new LlmResult(text, promptTokens, responseTokens, evalDurationNs, thinking);

            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrompu pendant un appel llama-server", e);
            } catch (Exception e) {
                lastCause = e;
                if (attempt < totalAttempts) {
                    int waitSec = retryPolicy.delaySeconds(attempt);
                    System.err.printf("[llama-server] tentative %d/%d échouée (%s), reprise dans %ds…%n",
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
            "llama-server : échec après " + totalAttempts + " tentative(s) : " + lastCause.getMessage(), lastCause);
    }

    // -------------------------------------------------------------------------
    // Appel LLM — mode stream (SSE : "data: {json}" par chunk, "data: [DONE]" en fin)
    // -------------------------------------------------------------------------

    private LlmResult sendStreaming(String systemPrompt, String userPrompt, double temperature,
                                     String agentLabel, GenerationOptions options,
                                     Consumer<PartialGeneration> onPartial) {
        LlamaServerChatRequest req = buildChatRequest(systemPrompt, userPrompt, temperature, options);
        req.stream = true;

        HttpRequest request;
        try {
            String body = JSON.writeValueAsString(req);
            request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Construction requête llama-server (stream) échouée : " + e.getMessage(), e);
        }

        int       totalAttempts = retryPolicy.retryCount() + 1;
        Exception lastCause     = null;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                return executeStreaming(request, agentLabel, onPartial);
            } catch (ContextOverflowException e) {
                throw e;
            } catch (GenerationCancelledException e) {
                throw e;
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new GenerationCancelledException("Génération interrompue par l'utilisateur");
                }
                lastCause = e;
                if (attempt < totalAttempts) {
                    int waitSec = retryPolicy.delaySeconds(attempt);
                    System.err.printf("[llama-server] tentative stream %d/%d échouée (%s), reprise dans %ds…%n",
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
            "llama-server stream : échec après " + totalAttempts + " tentative(s) : " + lastCause.getMessage(), lastCause);
    }

    private LlmResult executeStreaming(HttpRequest request, String agentLabel,
                                        Consumer<PartialGeneration> onPartial) {
        CompletableFuture<HttpResponse<InputStream>> cf =
            http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());

        HttpResponse<InputStream> response;
        try {
            response = cf.get(firstTokenTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cf.cancel(true);
            throw new RuntimeException("Timeout connexion llama-server (" + (firstTokenTimeoutMs / 1000) + "s)");
        } catch (ExecutionException e) {
            throw new RuntimeException("Erreur connexion llama-server : " + e.getCause().getMessage(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrompu en attente de connexion llama-server");
        }

        if (response.statusCode() != 200) {
            try (InputStream is = response.body()) {
                String errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                if (isContextOverflow(errorBody))
                    throw new ContextOverflowException(errorBody, contextWindowSize);
                throw new RuntimeException("llama-server HTTP " + response.statusCode() + " : " + errorBody);
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new RuntimeException("llama-server HTTP " + response.statusCode());
            }
        }

        StringBuilder content      = new StringBuilder();
        StringBuilder thinking     = new StringBuilder();
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
                if (!line.startsWith("data:")) continue;
                String payload = line.substring(5).trim();
                if (payload.equals("[DONE]")) break;
                firstToken = false;

                LlamaServerChatResponse chunk = JSON.readValue(payload, LlamaServerChatResponse.class);
                if (chunk.error != null && !chunk.error.isBlank()) {
                    if (isContextOverflow(chunk.error))
                        throw new ContextOverflowException(chunk.error, contextWindowSize);
                    throw new RuntimeException("llama-server streaming error : " + chunk.error);
                }
                LlamaServerMessage delta = chunk.choices != null && !chunk.choices.isEmpty()
                    ? chunk.choices.get(0).delta : null;
                if (delta != null && delta.content != null && !delta.content.isEmpty()) {
                    content.append(delta.content);
                    onPartial.accept(new PartialGeneration(thinking.toString(), content.toString()));
                }
                if (delta != null && delta.reasoningContent != null && !delta.reasoningContent.isEmpty()) {
                    thinking.append(delta.reasoningContent);
                    onPartial.accept(new PartialGeneration(thinking.toString(), content.toString()));
                }
                if (chunk.timings != null) {
                    promptTokens = chunk.timings.promptN;
                    evalTokens   = chunk.timings.predictedN;
                    evalDuration = (long) (chunk.timings.predictedMs * 1_000_000);
                }
                if (chunk.usage != null) {
                    if (chunk.usage.promptTokens > 0)     promptTokens = chunk.usage.promptTokens;
                    if (chunk.usage.completionTokens > 0) evalTokens   = chunk.usage.completionTokens;
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lecture stream llama-server : " + e.getMessage(), e);
        }

        logLlmCall(agentLabel, promptTokens, evalTokens, evalDuration, wantedThink(null));
        return new LlmResult(content.toString(), promptTokens, evalTokens, evalDuration, thinking.toString());
    }

    /**
     * Lit une ligne avec timeout — ferme le flux sur expiration pour débloquer le thread lecteur.
     * Même mécanique que OllamaAdapter.readLineWithTimeout (dupliquée volontairement : code
     * court, testé indépendamment, on évite de risquer une régression sur le flux Ollama pour un
     * partage d'utilitaire).
     */
    private static String readLineWithTimeout(BufferedReader reader, InputStream raw, long timeoutMs) {
        Future<String> future = STREAM_EXECUTOR.submit(reader::readLine);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            try { raw.close(); } catch (IOException ignored) {}
            throw new RuntimeException("Timeout stream llama-server (" + timeoutMs + "ms sans nouveau token)");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Erreur lecture stream : " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            try { raw.close(); } catch (IOException ignored) {}
            throw new RuntimeException("Interrompu pendant la lecture stream llama-server");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers communs
    // -------------------------------------------------------------------------

    private LlamaServerChatRequest buildChatRequest(String systemPrompt, String userPrompt, double temperature,
                                                      GenerationOptions options) {
        LlamaServerChatRequest req = new LlamaServerChatRequest();
        req.model         = model;
        req.temperature   = temperature;
        req.topK          = options.topK() != null ? options.topK() : topK;
        req.topP          = options.topP() != null ? options.topP() : topP;
        req.repeatPenalty = options.repeatPenalty() != null ? options.repeatPenalty() : repeatPenalty;
        req.minP          = options.minP();
        int effectiveNumPredict = options.maxTokens() != null ? options.maxTokens() : numPredict;
        if (effectiveNumPredict >= 0) req.nPredict = effectiveNumPredict;
        if (options.stopSequences() != null && !options.stopSequences().isEmpty())
            req.stop = options.stopSequences();
        req.messages = List.of(
            new LlamaServerMessage("system", systemPrompt),
            new LlamaServerMessage("user",   userPrompt)
        );
        return req;
    }

    private static LlamaServerMessage firstMessage(LlamaServerChatResponse resp) {
        if (resp.choices == null || resp.choices.isEmpty()) return null;
        return resp.choices.get(0).message;
    }

    private static int promptTokens(LlamaServerChatResponse resp) {
        if (resp.usage != null && resp.usage.promptTokens > 0) return resp.usage.promptTokens;
        return resp.timings != null ? resp.timings.promptN : 0;
    }

    private static int responseTokens(LlamaServerChatResponse resp) {
        if (resp.usage != null && resp.usage.completionTokens > 0) return resp.usage.completionTokens;
        return resp.timings != null ? resp.timings.predictedN : 0;
    }

    private static long evalDurationNs(LlamaServerChatResponse resp) {
        return resp.timings != null ? (long) (resp.timings.predictedMs * 1_000_000) : 0;
    }

    /** Ce que l'appelant a réellement demandé — jamais null. Utilisé pour la traçabilité (logs) ;
     * llama-server n'a pas de champ requête équivalent au think:true/false d'Ollama (voir
     * LlamaServerMessage.reasoningContent, lu tel quel s'il est présent en réponse). */
    private Boolean wantedThink(Boolean thinkOverride) {
        return thinkOverride != null ? thinkOverride : (think ? Boolean.TRUE : Boolean.FALSE);
    }

    private void logLlmCall(String agentLabel, int promptTokens, int evalTokens, long evalDurationNs, Boolean think) {
        long   ms  = evalDurationNs > 0 ? evalDurationNs / 1_000_000L : 0;
        double tps = evalTokens > 0 && evalDurationNs > 0
            ? evalTokens / (evalDurationNs / 1_000_000_000.0) : 0;
        log.llmCall(agentLabel, ms, promptTokens, evalTokens, tps, think);
    }

    /** Best-effort : GET /props n'expose pas param_size/quantization, seulement model_path. */
    private void fetchModelInfo() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/props"))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return;
            JsonNode root = JSON.readTree(resp.body());
            cachedModelPath = root.path("model_path").asText("");
        } catch (Exception e) {
            System.err.printf("[llama-server] /props ignoré : %s%n", e.getMessage());
        }
    }

    private static boolean isContextOverflow(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("truncat")
            || lower.contains("too large")
            || (lower.contains("context") && (lower.contains("exceed") || lower.contains("limit") || lower.contains("overflow")));
    }
}
