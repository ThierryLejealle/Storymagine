package storymagine.redacteur.coeur.domaine.agent.writer.planfidelitycritic;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Verifies that all planned beats are developed in the written text.
 * One MANQUANT per missing beat; score computed from failure count.
 */
public class PlanFidelityCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un editeur qui verifie qu'un texte exploite tous les beats d'un plan de sequence.
            Un beat est exploite si son intention narrative est developpee dans le texte — cherche l'essence du beat, pas ses mots exacts. Une allusion fugace ne compte pas.
            Examine chaque beat individuellement.
            Pour chaque beat absent ou non developpe, ecris une ligne :
            MANQUANT: [le beat tel quel]
            Si tous les beats sont exploites : ecrire TOUT_OK — rien d'autre.
            Rien d'autre : ni introduction ni conclusion. En francais.""";

    private static final String AGENT_NAME = "SequencePlanFidelityCritic";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanFidelityCritic(ModelCallPort llm) {
        this.llm = llm;
    }

    public PlanFidelityCriticOutput call(PlanFidelityCriticInput input) {
        String beatsBlock = input.beats().stream()
                .map(b -> "- " + b)
                .collect(Collectors.joining("\n"));
        String user = "### Beats du plan\n" + beatsBlock
                + "\n\n### Texte rédigé\n" + trunc(input.sequenceText(), llm.contextWindow() * 4 / 2)
                + "\n\nVérifie que chaque beat est développé dans le texte.";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel())).text();
        List<String> failures = parseFailures(raw);
        int score = Math.max(1, 10 - failures.size() * 3);
        return new PlanFidelityCriticOutput(failures, score);
    }

    private static List<String> parseFailures(String response) {
        List<String> failures = new ArrayList<>();
        if (response == null || response.trim().startsWith("TOUT_OK")) return failures;
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
