package storymagine.testllm.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Fournit un LongSupplier qui interroge /api/ps pour lire la VRAM d'un modèle chargé.
 */
class VramPoller {

    private static final ObjectMapper JSON = new ObjectMapper();

    static LongSupplier forModel(String baseUrl, String model) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        return () -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/ps"))
                        .GET()
                        .timeout(Duration.ofSeconds(3))
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                JsonNode root = JSON.readTree(resp.body());
                for (JsonNode m : root.path("models")) {
                    String name = m.path("name").asText("");
                    if (name.equals(model) || name.startsWith(model) || model.startsWith(name.split(":")[0])) {
                        long bytes = m.path("size_vram").asLong(0);
                        if (bytes > 0) return bytes / (1024 * 1024);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {}
            return -1L;
        };
    }
}
