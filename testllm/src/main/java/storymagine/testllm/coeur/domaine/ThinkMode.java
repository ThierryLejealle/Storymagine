package storymagine.testllm.coeur.domaine;

import java.util.List;

/**
 * Choix utilisateur des variantes thinking à tester pour un modèle.
 */
public enum ThinkMode {
    SANS, AVEC, LES_DEUX;

    /** Valeurs de "think" à tester pour ce mode, compte tenu du support réel du modèle. */
    public List<Boolean> versions(boolean modeleSupporteThinking) {
        if (!modeleSupporteThinking) return List.of(false);
        return switch (this) {
            case SANS      -> List.of(false);
            case AVEC      -> List.of(true);
            case LES_DEUX  -> List.of(false, true);
        };
    }
}
