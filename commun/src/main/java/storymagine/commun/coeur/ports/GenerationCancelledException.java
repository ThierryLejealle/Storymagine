package storymagine.commun.coeur.ports;

/**
 * Levée quand une génération LLM en cours est interrompue via Thread.interrupt() (voir
 * OllamaAdapter.sendStreaming) — un arrêt volontaire, pas une erreur réseau : les appelants ne
 * doivent jamais retenter après l'avoir reçue.
 */
public class GenerationCancelledException extends RuntimeException {

    public GenerationCancelledException(String message) {
        super(message);
    }
}
