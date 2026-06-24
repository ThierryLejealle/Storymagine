package storymagine.commun.coeur.ports;

import java.util.List;

/**
 * Port de gestion du cycle de vie et d'interrogation des modèles LLM.
 */
public interface ModelLifecyclePort {

    /** Vérifie que le modèle répond avant de démarrer une génération. */
    void probe();

    /** Demande au LLM de décharger le modèle de la mémoire immédiatement. */
    void unload();

    /** Nom du modèle actif. */
    String modelName();

    /** Taille de la fenêtre de contexte en tokens. */
    int contextWindow();

    /** Taille des paramètres déclarée, ex. "12.2B". Vide si inconnue. */
    String modelParamSize();

    /** Niveau de quantization, ex. "Q4_K_M". Vide si inconnu. */
    String modelQuantization();

    /** Famille/architecture du modèle, ex. "gemma3". Vide si inconnue. */
    String modelFamily();

    /** Bloc de texte décrivant le modèle (paramètres, quantization, VRAM…). */
    String modelInfoBlock();

    /** Indique si le modèle supporte le mode thinking. */
    boolean supportsThinking();

    /** Liste les modèles disponibles, filtrés par taille max en octets (0 = sans filtre). */
    List<ModelEntry> listModels(long maxBytes);

    /** Détecte la fenêtre de contexte native d'un modèle via l'API. */
    int detectContext(String model);
}
