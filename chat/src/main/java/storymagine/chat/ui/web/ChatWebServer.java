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
import storymagine.chat.coeur.service.ExchangeProgressListener;
import storymagine.commun.coeur.ports.GenerationCancelledException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        server.createContext("/set-present", this::handleSetPresent);
        server.createContext("/set-interjecting", this::handleSetInterjecting);
        server.createContext("/reload-scenario", this::handleReloadScenario);
        server.createContext("/restart", this::handleRestart);
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

    /**
     * NDJSON en chunked transfer (voir NdjsonWriter) plutot qu'un seul JSON en fin de tour : une
     * reponse a plusieurs PNJ peut prendre 30s+ au total, autant montrer chaque replique grandir en
     * temps reel plutot que de faire attendre le tour entier (voir chat.html : sendMessage() lit le
     * flux ligne a ligne). Trois formes de ligne possibles pendant la generation (voir
     * ExchangeProgressListener) : un PartialReplyEvent (npcId/textSoFar) a chaque fragment de texte
     * du modele, puis un ChatTurn complet une fois la replique finie — le client remplace alors
     * l'apercu progressif par le rendu final, identique dans les deux cas. Derniere ligne
     * recapitulative — meme forme que /history — traitee par applyExchange() comme avant, ses
     * newTurns() etant vide puisque tout a deja ete envoye au fil de l'eau. Statut HTTP toujours
     * 200 : le client ne regarde jamais le code de statut, seulement les champs presents sur
     * chaque ligne (textSoFar/speaker/stopped/error).
     */
    private void handleMessage(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).strip();
        if (body.isEmpty()) {
            writeJson(ex, 200, currentView(List.of(), 0, false, false));
            return;
        }
        beginGeneration();
        ex.getResponseHeaders().add("Content-Type", "application/x-ndjson; charset=utf-8");
        ex.sendResponseHeaders(200, 0);
        try (var os = ex.getResponseBody()) {
            NdjsonWriter out = new NdjsonWriter(os);
            int[] streamedCount = {0};
            try {
                ChatTurnResult result = service.sendMessage(chatScenariosRoot, session, body, new ExchangeProgressListener() {
                    @Override public void onPartialReply(String npcId, String textSoFar) {
                        out.writeUnchecked(new PartialReplyEvent(npcId, textSoFar));
                    }
                    @Override public void onTurnReady(ChatTurn turn) {
                        streamedCount[0]++;
                        out.writeUnchecked(turn);
                    }
                });
                lastPromptTokens = result.promptTokens();
                out.write(currentView(List.of(), result.replacedTurnCount(), result.compacted(), result.actAdvanced()));
            } catch (GenerationCancelledException e) {
                // sendMessage() a deja retire tout ce tour (y compris le tour joueur, voir
                // ChatServiceImpl) — le client doit faire disparaitre exactement les tours deja
                // recus via le flux (streamedCount), pas juste le tour joueur optimiste.
                out.write(new StoppedView(true, streamedCount[0]));
            } catch (Exception e) {
                out.write(new ErrorView(e.getMessage()));
            }
        } finally {
            endGeneration();
        }
    }

    /**
     * npcId absent : regenere tout le tour (bouton global). npcId present : ne regenere qu'a
     * partir de la reponse de ce PNJ (bouton par bulle, voir chat.html) — voir
     * ChatService.retry(fromNpcId) pour pourquoi ca peut aussi redefaire des repliques suivantes.
     */
    private void handleRetry(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String npcId = parseFormBody(nullToEmpty(ex.getRequestURI().getQuery())).get("npcId");
        beginGeneration();
        try {
            ChatTurnResult result = npcId == null || npcId.isBlank()
                ? service.retry(chatScenariosRoot, session)
                : service.retry(chatScenariosRoot, session, npcId);
            lastPromptTokens = result.promptTokens();
            // seules les repliques sont remplacees : newTurns() inclut aussi le tour joueur
            // inchange, on ne renvoie donc que les repliques pour ne pas le faire dupliquer cote UI.
            writeJson(ex, 200, currentView(result.replyTurns(), result.replacedTurnCount(),
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

    /** Mutes/unmutes one Cast member (a vignette click). No-op (see ChatSession.setPresent) if it would mute the last present Npc. */
    private void handleSetPresent(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        Map<String, String> fields = parseFormBody(nullToEmpty(ex.getRequestURI().getQuery()));
        String id = fields.get("id");
        boolean present = Boolean.parseBoolean(fields.get("present"));
        service.setNpcPresent(chatScenariosRoot, session, id, present);
        writeJson(ex, 200, currentView(List.of(), 0, false, false));
    }

    /** Opts a Cast member in/out of unprompted interjections (a vignette's 💬 icon). No "keep at least one" guard, unlike presence. */
    private void handleSetInterjecting(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        Map<String, String> fields = parseFormBody(nullToEmpty(ex.getRequestURI().getQuery()));
        String id = fields.get("id");
        boolean interjecting = Boolean.parseBoolean(fields.get("interjecting"));
        service.setNpcInterjecting(chatScenariosRoot, session, id, interjecting);
        writeJson(ex, 200, currentView(List.of(), 0, false, false));
    }

    /** Refresh button — re-reads scenario.txt and every character .txt from disk, keeps the conversation so far. */
    private void handleReloadScenario(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        try {
            service.reloadScenario(chatScenariosRoot, session);
            writeJson(ex, 200, currentView(List.of(), 0, false, false));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        }
    }

    /**
     * Bouton "Recommencer" — remplacement complet de l'etat (comme /load-save), la reponse a la
     * meme forme que /history pour que le client refasse un rendu complet. Le client confirme avec
     * le joueur avant d'appeler ce point d'entree (voir chat.html) — l'ecrasement est donc toujours
     * un choix explicite, pas un accident a rattraper.
     */
    private void handleRestart(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        try {
            service.restartSession(chatScenariosRoot, session);
            lastPromptTokens = 0;
            writeJson(ex, 200, currentView(List.of(), 0, false, false));
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorView(e.getMessage()));
        }
    }

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
     * maxTokens=2500) plutôt qu'en JSON — on évite ainsi de désérialiser un record Jackson côté
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
            parseDouble(fields.get("topP")), parseDouble(fields.get("repeatPenalty")),
            parseDouble(fields.get("interjectionChance")));
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
            currentActTitle(), session.generationSettings(), castView());
    }

    private List<NpcView> castView() {
        var present = session.presentNpcIds();
        var interjecting = session.interjectingNpcIds();
        return session.scenario().cast().npcs().stream()
            .map(npc -> new NpcView(npc.id(), npc.label(), present.contains(npc.id()), interjecting.contains(npc.id())))
            .toList();
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

    /** One growing fragment of an in-progress Npc reply — see ExchangeProgressListener.onPartialReply. */
    private record PartialReplyEvent(String npcId, String textSoFar) {}

    private static void writeJson(HttpExchange ex, int status, Object payload) throws IOException {
        byte[] body = JSON.writeValueAsBytes(payload);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (var os = ex.getResponseBody()) { os.write(body); }
    }

    /**
     * One JSON object per line (NDJSON), flushed right away so the client's streaming reader sees
     * it immediately instead of buffered — see handleMessage. writeUnchecked wraps IOException
     * (broken pipe, client navigated away mid-generation) so it can be used from inside a
     * Consumer&lt;ChatTurn&gt; callback, which can't declare a checked exception.
     */
    private static final class NdjsonWriter {
        private final OutputStream os;

        NdjsonWriter(OutputStream os) { this.os = os; }

        void write(Object payload) throws IOException {
            os.write(JSON.writeValueAsBytes(payload));
            os.write('\n');
            os.flush();
        }

        void writeUnchecked(Object payload) {
            try { write(payload); } catch (IOException e) { throw new UncheckedIOException(e); }
        }
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
