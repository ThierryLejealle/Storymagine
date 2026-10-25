package storymagine.commun.infra;

import storymagine.commun.coeur.ports.LogPort;

/**
 * Configuration de l'adaptateur llama-server et fabrique d'instances LlamaServerAdapter.
 * Contrairement à Ollama, llama-server sert un seul modèle déjà chargé au lancement de son
 * process : pas de catalogue de modèles interrogeable, pas de redimensionnement de contexte à la
 * volée (contextWindowSize est fixé au lancement du process, non modifiable à chaud) — voir
 * LlamaServerAdapter. Le programme ne démarre ni n'arrête jamais ce process : il appelle un
 * serveur déjà lancé par l'utilisateur à baseUrl.
 */
public record LlamaServerConfig(
    String      baseUrl,
    int         contextWindowSize,
    int         topK,
    double      topP,
    double      repeatPenalty,
    int         numPredict,
    boolean     streamMode,
    int         timeoutMs,              // mode sync : timeout total de la requête
    int         firstTokenTimeoutMs,    // mode stream : chargement + prefill → premier token
    int         interTokenTimeoutMs,    // mode stream : délai max entre deux tokens consécutifs
    RetryPolicy retryPolicy
) {

    public LlamaServerAdapter adapter(String model) {
        return adapter(model, false, LogPort.NOOP);
    }

    public LlamaServerAdapter adapter(String model, LogPort log) {
        return adapter(model, false, log);
    }

    public LlamaServerAdapter adapter(String model, boolean think) {
        return adapter(model, think, LogPort.NOOP);
    }

    public LlamaServerAdapter adapter(String model, boolean think, LogPort log) {
        return new LlamaServerAdapter(baseUrl, model, contextWindowSize,
            topK, topP, repeatPenalty, numPredict,
            streamMode, timeoutMs, firstTokenTimeoutMs, interTokenTimeoutMs,
            think, retryPolicy, log);
    }
}
