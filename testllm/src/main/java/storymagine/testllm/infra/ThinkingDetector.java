package storymagine.testllm.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Détecte si un modèle Ollama supporte le thinking via /api/show capabilities[].
 * Appel autonome, sans nécessiter probe().
 */
class ThinkingDetector {

    private static final ObjectMapper JSON = new ObjectMapper();

    static boolean detect(String baseUrl, String model) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            String body = "{\"name\":\"" + model.replace("\"", "\\\"") + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/show"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return false;
            JsonNode caps = JSON.readTree(resp.body()).path("capabilities");
            if (caps.isArray()) {
                for (JsonNode cap : caps) {
                    if ("thinking".equals(cap.asText())) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
