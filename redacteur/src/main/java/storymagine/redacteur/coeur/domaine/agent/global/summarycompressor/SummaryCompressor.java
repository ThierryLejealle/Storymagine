package storymagine.redacteur.coeur.domaine.agent.global.summarycompressor;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

/**
 * Condenses the running story summary to roughly half its size once it has grown past
 * SummaryBudget.wordBudget(). Source: Story.compressSummary, triggered by ChapterSummaryStep.
 */
public class SummaryCompressor implements Agent {

    private static final String AGENT_NAME = "SummaryCompressor";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public SummaryCompressor(ModelCallPort llm) {
        this.llm = llm;
    }

    public SummaryCompressorOutput call(SummaryCompressorInput input) {
        int targetWords = wordCount(input.summary()) / 2;

        String system = """
            Tu es l'archiviste d'un roman. Le résumé cumulé de l'histoire est devenu trop long.
            Tu le condenses en préservant l'essentiel.

            INCLURE :
            - Événements et actions marquants (faits datés ou séquentiels)
            - État actuel des personnages principaux : où ils sont, ce qu'ils savent, leurs décisions
            - Changements de relation, d'alliance, de statut toujours valables aujourd'hui
            - Objets, lieux, éléments concrets importants pour la continuité

            EXCLURE :
            - Descriptions atmosphériques, ressenti émotionnel ponctuel
            - Un fait devenu obsolète : si un développement plus récent l'a remplacé
              (ex. une relation qui a évolué), ne garde que l'état le plus récent

            Prose factuelle. En français.""";

        String user = "RÉSUMÉ ACTUEL (à condenser) :\n" + input.summary()
            + "\n\nProduis une version condensée en " + targetWords + " mots maximum.";

        String raw = llm.generate(system, user, 0.4, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new SummaryCompressorOutput(raw);
    }

    private static int wordCount(String text) {
        return text == null || text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }
}
