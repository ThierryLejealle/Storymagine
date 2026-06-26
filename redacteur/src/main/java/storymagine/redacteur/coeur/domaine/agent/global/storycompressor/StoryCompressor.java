package storymagine.redacteur.coeur.domaine.agent.global.storycompressor;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

/**
 * Compresses the story history after each chapter.
 * Produces a growing factual summary injected into future chapter contexts.
 * Source: CompressorContext.compress.
 */
public class StoryCompressor implements Agent {

    private static final String AGENT_NAME = "StoryCompressor";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public StoryCompressor(ModelCallPort llm) {
        this.llm = llm;
    }

    public StoryCompressorOutput call(StoryCompressorInput input) {
        int maxWords = input.summaryBaseWords() + input.chapterIndex() * input.summaryWordsPerChapter();
        int ctx = llm.contextWindow();

        String system = """
            Tu es l'archiviste d'un roman. Après chaque chapitre, tu produis un résumé
            factuel compact qui servira de contexte au chapitre suivant.

            INCLURE :
            - Événements et actions qui ont eu lieu (faits datés ou séquentiels)
            - État actuel des personnages principaux : où ils sont, ce qu'ils savent, leurs décisions
            - Changements de relation, d'alliance, de statut
            - Objets, lieux, éléments concrets importants pour la continuité

            EXCLURE :
            - Descriptions atmosphériques (météo, lumière, ambiance) sauf si elles ont
              une signification narrative directe
            - Ressenti émotionnel interne ponctuel (humeur du moment, sensation fugace)
            - Répétitions de ce qui est déjà dans le résumé existant sans qu'il y ait évolution

            Intègre le résumé existant : garde l'essentiel, remplace ce qui a évolué.
            Prose factuelle. En français.""";

        int summaryBudget = ctx * 4 / 6;
        int chapterBudget = ctx * 4 / 2;

        String recentSummary = lastSentences(input.existingSummary(), summaryBudget);
        String user = "RÉSUMÉ EXISTANT:\n"
            + (recentSummary == null || recentSummary.isBlank() ? "(début de l'histoire)" : recentSummary)
            + "\n\nNOUVEAU CHAPITRE:\n" + trunc(input.newChapterText(), chapterBudget)
            + "\n\nProduis le résumé mis à jour en " + maxWords + " mots maximum.";

        String raw = llm.generate(system, user, 0.4, LlmCallContext.of(agentName(), agentLabel())).text();
        return new StoryCompressorOutput(raw);
    }

    private static String lastSentences(String text, int maxChars) {
        if (text == null || text.isBlank()) return "";
        if (text.length() <= maxChars) return text;
        String tail = text.substring(text.length() - maxChars);
        int dot = tail.indexOf('.');
        return dot >= 0 ? tail.substring(dot + 1).trim() : tail.trim();
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
