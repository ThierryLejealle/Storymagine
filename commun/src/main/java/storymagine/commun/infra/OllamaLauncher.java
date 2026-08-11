package storymagine.commun.infra;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Kills and restarts Ollama with a specific CUDA_VISIBLE_DEVICES setting. */
public class OllamaLauncher {

    private static final int WAIT_TIMEOUT_S = 60;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static boolean isReachable(String baseUrl) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/version"))
                    .GET().timeout(Duration.ofSeconds(3)).build();
            return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Tue tous les processus Ollama dans le bon ordre.
     * "ollama app.exe" en premier — sinon il relance ollama.exe automatiquement.
     */
    public static void killAll() throws Exception {
        System.out.printf("[%s] [Ollama] arret des process Ollama en cours...%n", ts());
        for (String name : List.of("ollama app.exe", "llama-server.exe", "ollama.exe")) {
            new ProcessBuilder("taskkill", "/f", "/im", name)
                    .redirectErrorStream(true).start().waitFor();
            Thread.sleep(300);
        }
        System.out.printf("[%s] [Ollama] attente liberation du port 11434...%n", ts());
        long deadline = System.currentTimeMillis() + 10_000;
        int  tick     = 0;
        while (System.currentTimeMillis() < deadline) {
            if (!isReachable("http://localhost:11434")) break;
            if (++tick % 4 == 0) {
                System.out.printf("[%s] [Ollama] port 11434 toujours occupe, attente...%n", ts());
            }
            Thread.sleep(500);
        }
        if (isReachable("http://localhost:11434")) {
            throw new RuntimeException(
                "Ollama n'a pas pu etre arrete dans les 10s — port 11434 toujours occupe. " +
                "Arretez Ollama manuellement et relancez.");
        }
        System.out.printf("[%s] [Ollama] port libere.%n", ts());
        Thread.sleep(500);
    }

    /**
     * Lance "ollama serve" avec CUDA_VISIBLE_DEVICES force, attend qu'il soit pret.
     *
     * @param cudaDevices ex. "0", "1", "0,1"
     * @param baseUrl     ex. "http://localhost:11434"
     */
    public static Process launch(String cudaDevices, String baseUrl) throws Exception {
        System.out.printf("[%s] [Ollama] lancement 'ollama serve' (CUDA_VISIBLE_DEVICES=%s)...%n", ts(), cudaDevices);
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "ollama serve");
        pb.environment().put("CUDA_VISIBLE_DEVICES", cudaDevices);
        pb.environment().put("OLLAMA_VULKAN", "false");
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        Thread drainer = new Thread(() -> {
            try { proc.getInputStream().transferTo(java.io.OutputStream.nullOutputStream()); }
            catch (Exception ignored) {}
        });
        drainer.setDaemon(true);
        drainer.start();

        waitUntilReady(baseUrl);
        return proc;
    }

    private static void waitUntilReady(String baseUrl) throws InterruptedException {
        System.out.printf("[%s] [Ollama] attente demarrage du serveur (max %ds)...%n", ts(), WAIT_TIMEOUT_S);
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_S * 1000L;
        long start    = System.currentTimeMillis();
        int  tick     = 0;
        while (System.currentTimeMillis() < deadline) {
            if (isReachable(baseUrl)) {
                System.out.printf("[%s] [Ollama] serveur pret (%ds).%n",
                    ts(), (System.currentTimeMillis() - start) / 1000);
                return;
            }
            if (++tick % 5 == 0) {
                System.out.printf("[%s] [Ollama] toujours en attente du serveur (%ds/%ds)...%n",
                    ts(), (System.currentTimeMillis() - start) / 1000, WAIT_TIMEOUT_S);
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException("Ollama n'a pas demarre dans les " + WAIT_TIMEOUT_S + "s");
    }

    private static String ts() {
        return LocalTime.now().format(TIME);
    }
}
