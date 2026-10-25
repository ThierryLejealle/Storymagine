package storymagine.commun.ui.cli;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.infra.LlamaServerAdapter;
import storymagine.commun.infra.LlamaServerConfig;
import storymagine.commun.infra.RetryPolicy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Flux de connexion à un llama-server déjà lancé par l'utilisateur — pas de menu GPU ni de choix
 * de modèle (un seul modèle déjà chargé par process, voir LlamaServerAdapter) : juste une
 * vérification de connexion et lecture des réglages dans les propriétés du module.
 */
public final class LlamaServerSetupCli {

    private LlamaServerSetupCli() {}

    /** Outcome of the setup : ready-to-use adapter, nom du modèle configuré, et la config d'origine. */
    public record Selection(LlamaServerAdapter llm, String modelName, LlamaServerConfig config) {}

    public static Selection run(Properties props, LogPort log) {
        LlamaServerConfig config    = buildConfig(props);
        String            modelName = props.getProperty("llamaserver.model-name", "");

        System.out.println("Connexion a llama-server (" + config.baseUrl() + ")...");
        if (!isReachable(config.baseUrl())) {
            System.err.println("ERREUR : llama-server non joignable a " + config.baseUrl() + ".");
            System.err.println("Lancez llama-server d'abord (voir llamaserver.base-url dans les proprietes).");
            System.exit(1);
        }
        System.out.println(">> llama-server pret"
            + (modelName.isBlank() ? "" : " (modele configure : " + modelName + ")"));

        return new Selection(config.adapter(modelName, log), modelName, config);
    }

    public static LlamaServerConfig buildConfig(Properties props) {
        String  baseUrl             = props.getProperty("llamaserver.base-url",                        "http://localhost:8100");
        int     ctxWindow           = Integer.parseInt(props.getProperty("llamaserver.context-window",                "32768"));
        int     topK                = Integer.parseInt(props.getProperty("llamaserver.top-k",                            "40"));
        double  topP                = Double.parseDouble(props.getProperty("llamaserver.top-p",                         "0.9"));
        double  repeatPenalty       = Double.parseDouble(props.getProperty("llamaserver.repeat-penalty",                "1.1"));
        int     numPredict          = Integer.parseInt(props.getProperty("llamaserver.num-predict",                       "-1"));
        boolean streamMode          = !"sync".equalsIgnoreCase(props.getProperty("llamaserver.mode",                 "stream"));
        int     timeoutMs           = Integer.parseInt(props.getProperty("llamaserver.timeout-ms",                   "600000"));
        int     firstTokenTimeoutMs = Integer.parseInt(props.getProperty("llamaserver.stream.first-token-timeout-ms", "300000"));
        int     interTokenTimeoutMs = Integer.parseInt(props.getProperty("llamaserver.stream.inter-token-timeout-ms",  "30000"));
        int     retryCount          = Integer.parseInt(props.getProperty("llamaserver.retry-count",                        "5"));
        int     retryDelay1         = Integer.parseInt(props.getProperty("llamaserver.retry-delay1",                      "15"));
        int     retryDelay2         = Integer.parseInt(props.getProperty("llamaserver.retry-delay2",                      "30"));
        int     retryDelay3         = Integer.parseInt(props.getProperty("llamaserver.retry-delay3",                      "60"));

        return new LlamaServerConfig(baseUrl, ctxWindow, topK, topP, repeatPenalty,
            numPredict, streamMode, timeoutMs, firstTokenTimeoutMs, interTokenTimeoutMs,
            new RetryPolicy(retryCount, retryDelay1, retryDelay2, retryDelay3));
    }

    private static boolean isReachable(String baseUrl) {
        try {
            HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .GET().timeout(Duration.ofSeconds(3)).build();
            return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        } catch (Exception ignored) {
            return false;
        }
    }
}
