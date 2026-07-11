package storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
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
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanFidelityCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanFidelityCriticOutput call(PlanFidelityCriticInput input) {
        String beatsBlock = input.beats().stream()
                .map(b -> "- " + b)
                .collect(Collectors.joining("\n"));
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Beats du plan", beatsBlock)
                .section("Texte rédigé", t.text(input.sequenceText(), llm.contextWindow() * 4 / 2, "sequenceText"))
                .raw("Vérifie que chaque beat est développé dans le texte.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
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
}
