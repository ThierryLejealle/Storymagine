package storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer;

import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

/**
 * Summarizes a single chapter into a compact, independent, factual summary — written once,
 * never re-touched afterwards. Source: Story.appendChapterSummary.
 */
public class ChapterSummarizer implements Agent {

    private static final String AGENT_NAME = "ChapterSummarizer";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterSummarizer(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    /** Exposes the adapter's context window so the orchestration layer can size the compaction threshold. */
    public int contextWindow() { return llm.contextWindow(); }

    public ChapterSummarizerOutput call(ChapterSummarizerInput input) {
        int targetWords = wordCount(input.chapterText()) / input.divisor();

        String system = """
            Tu es l'archiviste d'un roman. Tu produis un résumé factuel compact d'UN chapitre,
            qui rejoindra le résumé global de l'histoire.

            INCLURE :
            - Événements et actions qui ont eu lieu (faits datés ou séquentiels)
            - État des personnages à la fin du chapitre : où ils sont, ce qu'ils savent, leurs décisions
            - Changements de relation, d'alliance, de statut
            - Objets, lieux, éléments concrets importants pour la suite

            EXCLURE :
            - Descriptions atmosphériques (météo, lumière, ambiance) sauf si elles ont
              une signification narrative directe
            - Ressenti émotionnel interne ponctuel (humeur du moment, sensation fugace)

            Prose factuelle. En français.""";

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = "CHAPITRE:\n" + t.text(input.chapterText(), ctx * 4 / 2, "chapterText")
            + "\n\nProduis le résumé de ce chapitre en " + targetWords + " mots maximum.";
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.4, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new ChapterSummarizerOutput(raw);
    }

    private static int wordCount(String text) {
        return text == null || text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }
}
