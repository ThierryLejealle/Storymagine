package storymagine.chat.ui.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalystOutput;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalystOutput;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.service.ChatService;
import storymagine.chat.coeur.service.ChatTurnResult;
import storymagine.commun.coeur.ports.GenerationCancelledException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Minimal local HTTP server for the chat page — JDK's built-in HttpServer, no framework. Serves
 * the static vanilla-JS page and endpoints (GET /history, POST /message...) as JSON. A 2-thread
 * pool : session-mutating endpoints (/message, /retry, /undo...) still only ever have one call in
 * flight in practice (the page disables its own controls meanwhile — ChatSession is a plain
 * mutable POJO, not built for real concurrency) ; the second thread exists solely so POST /stop
 * can run WHILE one of those calls is blocked inside a generation, to interrupt it.
 */
public class ChatWebServer {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ChatService service;
    private final Path        chatScenariosRoot;
    private final ChatSession session;
    private final int         port;
    private final int         contextWindow;

    /** Estimated size (see ChatContextBudget) of the prompt the next turn would send — 0 before the first exchange. */
    private volatile int lastPromptTokens = 0;

    /** The thread currently blocked inside /message or /retry, if any — see handleStop. */
    private volatile Thread currentGenerationThread = null;

    public ChatWebServer(ChatService service, Path chatScenariosRoot, ChatSession session, int port) {
        this.service           = service;
        this.chatScenariosRoot = chatScenariosRoot;
        this.session           = session;
        this.port              = port;
        this.contextWindow     = service.contextWindow();
    }

    public void start() throws IOException {
        // Loopback uniquement — une session RP est un contenu prive, elle ne doit pas etre servie
        // au reste du LAN sans authentification.
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/",         this::handlePage);
        server.createContext("/history",  this::handleHistory);
        server.createContext("/message",  this::handleMessage);
        server.createContext("/next-act", this::handleNextAct);
        server.createContext("/previous-act", this::handlePreviousAct);
        server.createContext("/analyze-next-act", this::handleAnalyzeNextAct);
        server.createContext("/analyze-mind-state", this::handleAnalyzeMindState);
        server.createContext("/save",      this::handleSave);
        server.createContext("/saves",     this::handleListSaves);
        server.createContext("/load-save", this::handleLoadSave);
        server.createContext("/delete-save", this::handleDeleteSave);
        server.createContext("/retry",    this::handleRetry);
        server.createContext("/undo",     this::handleUndo);
        server.createContext("/settings", this::handleSettings);
        server.createContext("/stop",     this::handleStop);
        server.setExecutor(Executors.newFixedThreadPool(2));
        server.start();
    }

    private void handlePage(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        byte[] html = readResource("/web/chat.html");
        ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, html.length);
        try (var os = ex.getResponseBody()) { os.write(html); }
    }

    private void handleHistory(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        writeJson(ex, 200, currentView(List.of(), 0, false, false));
    }

    private void handleMessage(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).strip();
        if (body.isEmpty()) {
            writeJson(ex, 200, currentView(List.of(), 0, false, false));
            return;
        }
        beginGeneration();
        try {
            ChatTurnResult result = service.sendMessage(chatScenariosRoot, session, body);
            lastPromptTokens = result.promptTokens();
            writeJson(ex, 200, currentView(result.newTurns(), result.replacedTurnCount(), result.compacted(),
                result.actAdvanced()));
        } catch (GenerationCancelledException e) {
            // sendMessage() a deja retire le tour joueur orphelin (voir ChatServiceImpl) — rien
            // n'a ete affiche cote serveur pour ce tour, donc rien a faire disparaitre.
            writeJson(ex, 200, new StoppedView(true, 0));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        } finally {
            endGeneration();
        }
    }

    private void handleRetry(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        beginGeneration();
        try {
            ChatTurnResult result = service.retry(chatScenariosRoot, session);
            lastPromptTokens = result.promptTokens();
            // seule la reponse est remplacee : newTurns() inclut aussi le tour joueur inchange, on
            // ne renvoie donc que la reponse pour ne pas le faire dupliquer cote UI.
            writeJson(ex, 200, currentView(List.of(result.replyTurn()), result.replacedTurnCount(),
                result.compacted(), result.actAdvanced()));
        } catch (IllegalStateException e) {
            writeJson(ex, 409, new ErrorView(e.getMessage()));
        } catch (GenerationCancelledException e) {
            // retry() avait deja retire l'ancienne reponse avant l'appel LLM — le client doit
            // enlever la bulle correspondante, devenue obsolete cote serveur.
            writeJson(ex, 200, new StoppedView(true, 1));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        } finally {
            endGeneration();
        }
    }

    private void handleStop(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        Thread generating = currentGenerationThread;
        if (generating != null) generating.interrupt();
        writeJson(ex, 200, new StoppedView(generating != null, 0));
    }

    /** Purge un flag d'interruption residuel avant d'enregistrer ce thread comme annulable. */
    private void beginGeneration() {
        Thread.interrupted();
        currentGenerationThread = Thread.currentThread();
    }

    private void endGeneration() {
        currentGenerationThread = null;
    }

    private void handleUndo(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        Integer steps = parseInt(parseFormBody(nullToEmpty(ex.getRequestURI().getQuery())).get("steps"));
        int removed = service.undo(chatScenariosRoot, session, steps != null ? steps : 1);
        writeJson(ex, 200, currentView(List.of(), removed, false, false));
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private void handleNextAct(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        int sizeBefore = session.turns().size();
        boolean advanced = service.advanceAct(chatScenariosRoot, session);
        List<ChatTurn> newTurns = advanced ? session.turns().subList(sizeBefore, session.turns().size()) : List.of();
        writeJson(ex, 200, currentView(newTurns, 0, false, advanced));
    }

    /** Manual correction button — no turns are added or removed, only session.currentAct() changes. */
    private void handlePreviousAct(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        service.previousAct(chatScenariosRoot, session);
        writeJson(ex, 200, currentView(List.of(), 0, false, false));
    }

    /**
     * Pas de mutation de session ici : lecture seule, rien n'est ajouté à l'historique ni persisté
     * (voir NextActReadinessAnalyst.md). Passe par beginGeneration/endGeneration comme /message et
     * /retry pour que /stop puisse aussi interrompre cet appel s'il traîne.
     */
    private void handleAnalyzeNextAct(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        beginGeneration();
        try {
            NextActReadinessAnalystOutput result = service.analyzeNextActReadiness(chatScenariosRoot, session);
            writeJson(ex, 200, result);
        } catch (IllegalStateException e) {
            writeJson(ex, 409, new ErrorView(e.getMessage()));
        } catch (GenerationCancelledException e) {
            writeJson(ex, 200, new StoppedView(true, 0));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        } finally {
            endGeneration();
        }
    }

    /**
     * Pas de mutation de session ici non plus (voir NpcMindStateAnalyst.md) — toujours disponible,
     * pas de garde-fou d'etat comme handleAnalyzeNextAct (aucune precondition cote service).
     */
    private void handleAnalyzeMindState(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        beginGeneration();
        try {
            NpcMindStateAnalystOutput result = service.analyzeMindState(chatScenariosRoot, session);
            writeJson(ex, 200, result);
        } catch (GenerationCancelledException e) {
            writeJson(ex, 200, new StoppedView(true, 0));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        } finally {
            endGeneration();
        }
    }

    private void handleSave(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        writeJson(ex, 200, service.save(chatScenariosRoot, session));
    }

    private void handleListSaves(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        writeJson(ex, 200, service.listSavePoints(chatScenariosRoot, session.scenario()));
    }

    /**
     * Remplacement complet de l'etat de session (tours/resume/acte), pas un echange incremental —
     * la reponse reutilise la meme forme que /history pour que le client refasse un rendu complet
     * (voir chat.html: render(), pas applyExchange()).
     */
    private void handleLoadSave(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String id = parseFormBody(nullToEmpty(ex.getRequestURI().getQuery())).get("id");
        try {
            service.loadSavePoint(chatScenariosRoot, session, id);
            lastPromptTokens = 0;
            writeJson(ex, 200, currentView(List.of(), 0, false, false));
        } catch (IllegalArgumentException e) {
            writeJson(ex, 400, new ErrorView(e.getMessage()));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        }
    }

    private void handleDeleteSave(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String id = parseFormBody(nullToEmpty(ex.getRequestURI().getQuery())).get("id");
        try {
            service.deleteSavePoint(chatScenariosRoot, session.scenario(), id);
            writeJson(ex, 200, service.listSavePoints(chatScenariosRoot, session.scenario()));
        } catch (IllegalArgumentException e) {
            writeJson(ex, 400, new ErrorView(e.getMessage()));
        }
    }

    /**
     * GET renvoie les réglages courants de la session (nuls = valeur par défaut de
     * RoleplayNarrator) ; POST les remplace, en formulaire url-encoded (temperature=0.9&topK=40&
     * maxTokens=1200) plutôt qu'en JSON — on évite ainsi de désérialiser un record Jackson côté
     * entrée, seule la sérialisation (sortie) est utilisée ailleurs dans ce serveur. Un champ vide
     * ou absent revient à null (retour au défaut).
     */
    private void handleSettings(HttpExchange ex) throws IOException {
        if ("GET".equals(ex.getRequestMethod())) {
            writeJson(ex, 200, session.generationSettings());
            return;
        }
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).strip();
        Map<String, String> fields = parseFormBody(body);
        GenerationSettings settings = new GenerationSettings(
            parseDouble(fields.get("temperature")), parseInt(fields.get("topK")), parseInt(fields.get("maxTokens")),
            parseDouble(fields.get("minP")), parseBoolean(fields.get("showThinking")),
            parseDouble(fields.get("topP")), parseDouble(fields.get("repeatPenalty")));
        session.updateGenerationSettings(settings);
        writeJson(ex, 200, settings);
    }

    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> fields = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            fields.put(key, value);
        }
        return fields;
    }

    private static Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Double.valueOf(raw); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Integer.valueOf(raw); } catch (NumberFormatException e) { return null; }
    }

    private static Boolean parseBoolean(String raw) {
        return raw == null || raw.isBlank() ? null : Boolean.valueOf(raw);
    }

    private ChatHistoryView currentView(List<ChatTurn> newTurns, int removedTurnCount, boolean compacted,
                                         boolean actAdvanced) {
        return new ChatHistoryView(session.scenario().name(), session.turns(), newTurns, removedTurnCount, compacted,
            lastPromptTokens, contextWindow, session.currentAct(), session.scenario().acts().size(), actAdvanced,
            currentActTitle(), session.generationSettings());
    }

    /** Display-only title of the current act (see ScenarioAct.title), null if there is none active. */
    private String currentActTitle() {
        int act = session.currentAct();
        var acts = session.scenario().acts();
        return act > 0 && act <= acts.size() ? acts.get(act - 1).title() : null;
    }

    private record ErrorView(String error) {}

    /** removedTurnCount tells the client how many already-shown bubbles are now stale — see /stop. */
    private record StoppedView(boolean stopped, int removedTurnCount) {}

    private static void writeJson(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] body = JSON.writeValueAsBytes(payload);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (var os = ex.getResponseBody()) { os.write(body); }
    }

    private static byte[] readResource(String classpath) {
        try (InputStream is = ChatWebServer.class.getResourceAsStream(classpath)) {
            if (is == null) throw new IllegalStateException("Ressource introuvable : " + classpath);
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
