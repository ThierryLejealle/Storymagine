package storymagine.testllm.coeur.service;

import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.testllm.coeur.domaine.BenchRun;
import storymagine.testllm.coeur.domaine.ThinkMode;

import java.nio.file.Path;
import java.util.List;

/**
 * Point d'entrée du coeur testllm.
 */
public interface TestLlmService {

    /** Lance un benchmark sur tous les modèles Ollama disponibles (filtre taille appliqué). */
    BenchRun benchTous(Path outputDir, ThinkMode thinkMode);

    /** Lance un benchmark sur les modèles favoris définis dans la configuration. */
    BenchRun benchFavoris(Path outputDir, ThinkMode thinkMode);

    /** Lance un benchmark sur un modèle précis. */
    BenchRun benchModele(String modele, Path outputDir, ThinkMode thinkMode);

    /** Retourne la liste de tous les modèles Ollama disponibles (avec taille). */
    List<ModelEntry> listerModeles();

    /** Retourne les modèles favoris configurés. */
    List<String> listerFavoris();
}
