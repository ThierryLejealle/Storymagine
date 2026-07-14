package storymagine.commun.coeur.ports;

import java.util.List;

/**
 * Per-call overrides layered on top of an adapter's own configured defaults for a single
 * generate() call — null/empty means "keep the adapter's default for this field".
 * stopSequences interrupts generation as soon as one of them appears in the output.
 * maxTokens caps generation length (Ollama's num_predict) — a belt-and-suspenders safety net
 * against a runaway/degenerate generation, independent of the module-wide default (often
 * unbounded, see redacteur.properties/chat.properties ollama.num-predict).
 * topK/topP/repeatPenalty override the adapter's own sampling defaults for this call only. minP
 * is never defaulted by the adapter (omitted from the request, letting Ollama's own default
 * apply) unless a caller sets it here — only RoleplayNarrator does, see its own default there.
 */
public record GenerationOptions(List<String> stopSequences, Integer maxTokens, Integer topK, Double topP,
                                 Double minP, Double repeatPenalty) {

    public static final GenerationOptions NONE = new GenerationOptions(List.of(), null, null, null, null, null);

    public static GenerationOptions stopSequences(List<String> stopSequences) {
        return new GenerationOptions(stopSequences, null, null, null, null, null);
    }

    public static GenerationOptions maxTokens(int maxTokens) {
        return new GenerationOptions(List.of(), maxTokens, null, null, null, null);
    }
}
