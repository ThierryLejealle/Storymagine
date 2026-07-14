package storymagine.commun.coeur.ports;

/**
 * Port d'appel LLM en mode "brut" : un seul bloc de texte envoyé en continuation, sans rôles
 * system/user ni template de chat imposé. Capacité additionnelle de l'adaptateur Ollama (voir
 * OllamaAdapter#complete) — distincte de ModelCallPort, qui passe toujours par /api/chat avec des
 * rôles. À utiliser quand le Modelfile du modèle ne déclare aucun TEMPLATE de rôles et qu'on veut
 * coller à ce rendu brut plutôt que de le forcer dans un format chat qu'il n'attend pas.
 */
public interface RawCompletionPort {

    /**
     * @param prompt      texte complet déjà assemblé (règles, contexte, historique, réplique…)
     * @param temperature 0..1 — créativité
     * @param ctx         identité de l'appelant — utilisé pour la traçabilité
     * @return résultat généré par le LLM
     */
    LlmResult complete(String prompt, double temperature, LlmCallContext ctx);

    /** Nombre de tokens que le modèle peut traiter en un seul appel. */
    int contextWindow();
}
