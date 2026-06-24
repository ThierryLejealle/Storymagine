package storymagine.testllm.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public record OllamaPsInfo(long sizeBytes, long sizeVramBytes) {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** "7.7 Go" */
    public String sizeLabel() {
        return String.format("%.1f Go", sizeBytes / 1_073_741_824.0);
    }

    /** "25%/75% CPU/GPU" | "100% GPU" | "100% CPU" */
    public String processorLabel() {
        if (sizeBytes <= 0)             return "?";
        if (sizeVramBytes <= 0)         return "100% CPU";
        if (sizeVramBytes >= sizeBytes) return "100% GPU";
        int gpuPct = (int) Math.round(100.0 * sizeVramBytes / sizeBytes);
        int cpuPct = 100 - gpuPct;
        return cpuPct + "%/" + gpuPct + "% CPU/GPU";
    }

    public static Optional<OllamaPsInfo> query(String baseUrl, String model) {
        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/ps"))
                    .GET().timeout(Duration.ofSeconds(3)).build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode root = JSON.readTree(resp.body());
            for (JsonNode m : root.path("models")) {
                if (model.equals(m.path("name").asText())) {
                    long size     = m.path("size").asLong(0);
                    long sizeVram = m.path("size_vram").asLong(0);
                    return Optional.of(new OllamaPsInfo(size, sizeVram));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }
}
