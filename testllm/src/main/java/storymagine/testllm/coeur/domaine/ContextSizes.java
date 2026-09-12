package storymagine.testllm.coeur.domaine;

import java.util.ArrayList;
import java.util.List;

/**
 * Paliers de num_ctx testes par testContext (16k a 256k), bornes par le contexte natif du modele.
 */
public final class ContextSizes {

    private ContextSizes() {}

    private static final int[] PALIERS_K = {16, 32, 64, 96, 128, 192, 256};

    /**
     * Paliers <= maxCtxNatif ; si maxCtxNatif n'est pas deja atteint exactement par un palier,
     * il est ajoute en dernier pour obtenir un point de mesure exactement a la limite du modele.
     * maxCtxNatif <= 0 (detection impossible) : tous les paliers sont retournes tels quels.
     */
    public static List<Integer> pourModele(int maxCtxNatif) {
        List<Integer> result = new ArrayList<>();
        for (int k : PALIERS_K) {
            int ctx = k * 1024;
            if (maxCtxNatif > 0 && ctx > maxCtxNatif) break;
            result.add(ctx);
        }
        if (maxCtxNatif > 0 && (result.isEmpty() || result.get(result.size() - 1) != maxCtxNatif)) {
            result.add(maxCtxNatif);
        }
        return result;
    }
}
