package storymagine.commun.infra;

/** Informations sur le modèle Ollama : déclaratives (/api/show) + runtime (/api/ps). */
class OllamaModelInfo {

    // --- /api/show (avant chargement) ---
    String  parameterSize    = "";
    String  quantization     = "";
    String  family           = "";
    long    nativeCtxLength  = 0;
    int     embeddingLength  = 0;
    int     layerCount       = 0;
    int     headCount        = 0;
    boolean supportsThinking = false;

    // --- /api/ps (après premier appel) ---
    long diskBytes = 0;
    long vramBytes = 0;

    /**
     * effectiveSupportsThinking : la valeur masquée (voir OllamaAdapter.supportsThinking()), pas le
     * champ brut supportsThinking de cette classe (celui d'/api/show, jamais corrigé pour gemma4) —
     * un seul "ce modèle pense-t-il ?" doit sortir du port, jamais deux réponses contradictoires
     * selon la méthode appelée.
     */
    String formatDeclared(String modelName, int configCtx, boolean effectiveSupportsThinking) {
        StringBuilder sb = new StringBuilder();
        sb.append("  Modèle      : ").append(modelName).append("\n");
        if (!parameterSize.isEmpty())
            sb.append("  Paramètres  : ").append(parameterSize).append("\n");
        if (!quantization.isEmpty())
            sb.append("  Quantization: ").append(quantization).append("\n");
        if (!family.isEmpty())
            sb.append("  Architecture: ").append(family).append("\n");
        sb.append("  Contexte cfg: ").append(configCtx).append(" tokens");
        if (nativeCtxLength > 0)
            sb.append(" (natif modèle: ").append(nativeCtxLength).append(")");
        sb.append("\n");
        if (layerCount > 0) sb.append("  Couches     : ").append(layerCount).append("  |  ");
        if (headCount > 0)  sb.append("Têtes: ").append(headCount).append("  |  ");
        if (embeddingLength > 0) sb.append("Embedding: ").append(embeddingLength).append("\n");
        sb.append("  Thinking    : ").append(effectiveSupportsThinking ? "oui" : "non").append("\n");
        return sb.toString().stripTrailing();
    }

    String formatRuntime() {
        if (vramBytes <= 0 && diskBytes <= 0) return "";
        StringBuilder sb = new StringBuilder();
        if (diskBytes > 0) sb.append("  Taille disque: ").append(humanBytes(diskBytes)).append("\n");
        if (vramBytes > 0) sb.append("  VRAM occupée : ").append(humanBytes(vramBytes)).append("\n");
        return sb.toString().stripTrailing();
    }

    private static String humanBytes(long bytes) {
        if (bytes >= 1_073_741_824L) return String.format("%.2f Go", bytes / 1_073_741_824.0);
        if (bytes >= 1_048_576L)     return String.format("%.1f Mo", bytes / 1_048_576.0);
        return bytes + " o";
    }
}
