package storymagine.testllm.coeur.domaine;

import java.util.List;
import java.util.Map;

/**
 * Résultat complet d'une session de benchmark (tous modèles, toutes passes).
 */
public record BenchRun(
        List<BenchModeleResult>              modeles,
        Map<Integer, List<BenchPasseResult>> parPasse,
        List<PasseBench>                     passes) {}
