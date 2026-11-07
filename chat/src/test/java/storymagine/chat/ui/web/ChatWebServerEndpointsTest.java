package storymagine.chat.ui.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizer;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalyst;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalyst;
import storymagine.chat.coeur.domaine.agent.replyorigin.ReplyOriginAnalyst;
import storymagine.chat.coeur.domaine.agent.roleplaynarrator.RoleplayNarrator;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.service.ChatServiceImpl;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The read-only debug endpoints of the page (🐞 scenario sent, 🎲 picks, ❓ origin of a reply) answer
 * with real JSON over real HTTP, on a real service with a stubbed model. The servers started here are
 * never stopped (no stop API) ; they die with the test JVM.
 */
class ChatWebServerEndpointsTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static String call(String method, int port, String path) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        HttpRequest built = method.equals("POST") ? request.POST(HttpRequest.BodyPublishers.noBody()).build() : request.GET().build();
        HttpResponse<String> response = HTTP.send(built, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.statusCode() + " " + response.body();
    }

    private int startServer(Path root, String scenarioText, String styleText) throws IOException {
        Path dir = root.resolve("inn");
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/elena.txt"), "# Elena\nUne aubergiste bourrue.", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
        if (styleText != null) Files.writeString(dir.resolve("style.txt"), styleText, StandardCharsets.UTF_8);

        ModelCallPort llm = new ModelCallPort() {
            @Override public LlmResult generate(String system, String user, double temperature, LlmCallContext ctx) {
                return LlmResult.of("TRACED:\n[RIEN]\nUNTRACED:\n- un detail\nREADING:\nRien.");
            }
            @Override public int contextWindow() { return 32_768; }
        };
        ChatServiceImpl service = new ChatServiceImpl(new ChatFileStorageAdapter(), new RoleplayNarrator(llm),
            new ChatSummarizer(llm), new NextActReadinessAnalyst(llm), new NpcMindStateAnalyst(llm),
            new ReplyOriginAnalyst(llm), LogPort.NOOP, new Random(1));
        ChatScenario scenario = service.loadScenario(root, "inn");
        ChatSession session = service.openSession(root, scenario, true);

        int preferred;
        try (ServerSocket probe = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            preferred = probe.getLocalPort();
        }
        return new ChatWebServer(service, root, session, preferred).start();
    }

    @Test
    void restartWithABrokenScenarioIsRefusedWithTheErrorsAndLeavesTheRunAlone(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n\n# Un\nElena sert un verre.\n", null);
        Path scenarioFile = root.resolve("inn/scenario.txt");
        Files.writeString(scenarioFile, Files.readString(scenarioFile, StandardCharsets.UTF_8) + "{teleport elena}\n", StandardCharsets.UTF_8);

        String answer = call("POST", port, "/restart");

        assertTrue(answer.startsWith("500 "), answer);
        assertTrue(answer.contains("Recommencer annul"), answer);
        assertTrue(answer.contains("teleport"), answer);
        assertTrue(!Files.exists(root.resolve("inn/run/archive")), "rien n'est archive");
    }

    @Test
    void openSceneMakesTheNamedCharacterSpeakWithNoPlayerLineAndOnlyOnce(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n{npc-starts Elena}\n", null);

        String history = call("GET", port, "/history");
        assertTrue(history.contains("\"npcOpeningPending\":true") && history.contains("\"npcOpeningWithPen\":false"), history);

        String answer = call("POST", port, "/open-scene");

        assertTrue(answer.startsWith("200 "), answer);
        assertTrue(answer.contains("\"npcId\":\"elena\""), answer);
        assertTrue(!answer.contains("\"speaker\":\"PLAYER\""), "aucune bulle du joueur : " + answer);
        assertTrue(call("POST", port, "/open-scene").contains("Aucun personnage"), "une seule ouverture");
    }

    @Test
    void theScenarioSentToTheCharactersIsServedWithItsStyle(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n\n# Un\nElena sert un verre.\n", "Phrases courtes.");

        String answer = call("GET", port, "/scenario-prompt");

        assertTrue(answer.startsWith("200 "), answer);
        assertTrue(answer.contains("Une nuit d'orage."), answer);
        assertTrue(answer.contains("Elena sert un verre."), answer);
        assertTrue(answer.contains("Phrases courtes."), answer);
    }

    @Test
    void theScenarioSentWorksWithoutActsAndWithoutStyle(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n", null);

        String answer = call("GET", port, "/scenario-prompt");

        assertTrue(answer.startsWith("200 "), answer);
        assertTrue(answer.contains("Une nuit d'orage."), answer);
    }

    @Test
    void picksAreServedAsAnEmptyListWhenThereAreNone(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n", null);

        assertEquals("200 []", call("GET", port, "/picks"));
    }

    @Test
    void picksAreServedCopyByCopyWithTheItemsAndTheirPiecesApart(@TempDir Path root) throws Exception {
        Files.createDirectories(root.resolve("inn/picks"));
        Files.writeString(root.resolve("inn/picks/trial.txt"), "Pont.\n-\nUne verite.\n---\nEnigme.\n-\nUne reponse.", StandardCharsets.UTF_8);
        int port = startServer(root, "Une nuit d'orage.\n\n{repeat order trial}\n# Epreuve {n}\nElena : {item1} puis {item2}\n{end-repeat}\n", null);

        String answer = call("GET", port, "/picks");

        assertTrue(answer.startsWith("200 "), answer);
        assertTrue(answer.contains("\"group\":\"trial 1/2\"") && answer.contains("\"group\":\"trial 2/2\""), answer);
        assertTrue(answer.contains("\"labelled\":true"), answer);
        assertTrue(answer.contains("\"items\":[[\"Pont.\",\"Une verite.\"]]"), "les morceaux d'un element sont separes : " + answer);
        assertTrue(answer.contains("\"items\":[[\"Enigme.\",\"Une reponse.\"]]"), answer);
    }

    @Test
    void aClickOnACharacterStepsItsActivityAndTheViewTellsWhereItComesFrom(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n", null);

        String answer = call("POST", port, "/cycle-activity?id=elena");

        assertTrue(answer.startsWith("200 "), answer);
        assertTrue(answer.contains("\"level\":\"passive\""), answer);
        assertTrue(answer.contains("\"source\":\"forced\""), answer);
        assertTrue(answer.contains("\"scenarioLevel\":\"present\""), answer);
    }

    @Test
    void theCastViewCarriesThePublicSheetWithoutItsHeadingsAndNeverTheSecret(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n", null);
        Files.writeString(root.resolve("inn/characters/elena.txt"),
            "# Elena\nUne aubergiste bourrue.\n\n# SECRET\nElle cache une dette.", StandardCharsets.UTF_8);
        call("POST", port, "/reload-scenario");

        String answer = call("GET", port, "/history");

        assertTrue(answer.contains("\"publicSheet\":\"Une aubergiste bourrue.\""), answer);
        assertTrue(!answer.contains("dette"), "le secret n'est jamais envoye : " + answer);
    }

    @Test
    void headingLinesAreDroppedFromASheetText() {
        assertEquals("Une aubergiste.\n\nBourrue.",
            ChatWebServer.withoutHeadings("# Elena\n\nUne aubergiste.\n  ## Humeur\n\nBourrue.\n"));
    }

    @Test
    void theOriginOfAReplyAnswersWithAClearErrorBeforeAnyReply(@TempDir Path root) throws Exception {
        int port = startServer(root, "Une nuit d'orage.\n", null);

        String answer = call("POST", port, "/analyze-reply-origin");

        assertTrue(answer.startsWith("409 "), answer);
        assertTrue(answer.contains("error"), answer);
    }
}
