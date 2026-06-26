package storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verifies that a written sequence contains all the required elements from the checks list.
 * Only evaluates presence, not quality. Called only when checks is non-empty.
 * Source: SequenceCheckerContext.check.
 */
public class SequenceChecker implements Agent {

    private static final String SYSTEM = """
        Tu es un éditeur chargé de vérifier qu'une séquence narrative contient tous les éléments
        requis par son auteur.
        Ne juge pas la qualité littéraire — uniquement la présence effective des éléments.

        SEUIL DE PRÉSENCE : un élément n'est présent que s'il est développé dans au moins une phrase
        qui le traite directement. Une allusion fugace ou une mention en passant ne compte pas.

        Examine chaque élément de la liste individuellement.
        Pour chaque élément absent ou seulement effleuré, écris :
        MANQUANT: [élément] — absent
        ou
        MANQUANT: [élément] — présent mais non développé

        Si TOUS les éléments sont présents et développés : n'écris AUCUNE ligne MANQUANT:
        En français.""";

    private static final String AGENT_NAME = "SequenceChecker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public SequenceChecker(ModelCallPort llm) {
        this.llm = llm;
    }

    public SequenceCheckerOutput call(SequenceCheckerInput input) {
        int ctx = llm.contextWindow();
        String checksBlock = input.checks().stream().map(c -> "- " + c).collect(Collectors.joining("\n"));
        String descSection = (input.sequenceDescription() != null && !input.sequenceDescription().isBlank())
            ? "\n\n### Description de la séquence\n" + input.sequenceDescription()
            : "";
        String user = "### Texte de la séquence\n"       + trunc(input.sequenceText(), ctx * 4 / 3)
            + descSection
            + "\n\n### Éléments importants à vérifier\n" + checksBlock
            + "\n\nVérifie que chaque élément est présent dans le texte.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> failures = parseFailures(raw);
        int total = input.checks().size();
        int missing = failures.size();
        int score;
        if (missing == 0)          score = 10;
        else if (missing >= total) score = 1;
        else                       score = (int) Math.round(5.0 * (total - missing) / (total - 1));
        return new SequenceCheckerOutput(failures, score);
    }

    private List<String> parseFailures(String response) {
        List<String> failures = new ArrayList<>();
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (t.startsWith("MANQUANT:")) {
                String f = t.substring("MANQUANT:".length()).trim();
                if (!f.isBlank()) failures.add(f);
            }
        }
        return failures;
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
