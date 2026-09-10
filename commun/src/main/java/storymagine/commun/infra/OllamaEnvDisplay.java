package storymagine.commun.infra;

/**
 * Affiche au demarrage les deux variables d'environnement Ollama qui pilotent la gestion du
 * contexte et sa compression : OLLAMA_FLASH_ATTENTION (prerequis technique) et
 * OLLAMA_KV_CACHE_TYPE (quantization du cache KV, ex. f16/q8_0/q4_0). Lit l'environnement du
 * processus Java courant — coherent seulement si Ollama tourne dans le meme environnement
 * (meme terminal/session), pas s'il tourne comme service Windows avec ses propres variables.
 */
public class OllamaEnvDisplay {

    public static void print(String prefix) {
        System.out.printf("%sOLLAMA_FLASH_ATTENTION : %s%n", prefix, valueOr(System.getenv("OLLAMA_FLASH_ATTENTION")));
        System.out.printf("%sOLLAMA_KV_CACHE_TYPE   : %s%n", prefix, valueOr(System.getenv("OLLAMA_KV_CACHE_TYPE")));
    }

    private static String valueOr(String value) {
        return value == null || value.isBlank() ? "(non definie)" : value;
    }
}
