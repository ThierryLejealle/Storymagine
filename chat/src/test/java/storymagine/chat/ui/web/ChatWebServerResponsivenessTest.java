package storymagine.chat.ui.web;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.service.ChatService;
import storymagine.chat.coeur.service.ScenarioPromptView;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Long LLM calls (a reply, an analysis) must never keep the instant read-only debug endpoints waiting :
 * with only two worker threads, two analyses in flight left the next click on 🐞 hanging on "Chargement…".
 */
class ChatWebServerResponsivenessTest {

    @Test
    void aDebugClickIsAnsweredWhileSeveralLongCallsAreInFlight() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(3);
        ChatService service = (ChatService) Proxy.newProxyInstance(
            ChatService.class.getClassLoader(), new Class<?>[]{ChatService.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "contextWindow": return 0;
                    case "scenarioPrompt": return new ScenarioPromptView("Premise.", 0, 0, "", "", List.of());
                    case "analyzeMindState", "analyzeNextActReadiness", "analyzeLastReplyOrigin":
                        started.countDown();
                        release.await(20, TimeUnit.SECONDS); // a long LLM call
                        return null;
                    default: return null;
                }
            });
        int preferred;
        try (ServerSocket probe = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            preferred = probe.getLocalPort();
        }
        int port = new ChatWebServer(service, Path.of("."), null, preferred).start();
        HttpClient http = HttpClient.newHttpClient();

        for (String longCall : new String[] {"/analyze-mind-state?npcId=x", "/analyze-next-act", "/analyze-reply-origin"}) {
            http.sendAsync(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + longCall))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "les trois appels longs doivent etre en cours");

        HttpResponse<String> answer = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/scenario-prompt"))
            .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
        release.countDown();

        assertEquals(200, answer.statusCode());
        assertTrue(answer.body().contains("Premise."), answer.body());
    }
}
