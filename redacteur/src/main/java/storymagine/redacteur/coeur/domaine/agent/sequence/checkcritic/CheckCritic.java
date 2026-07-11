package storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic;

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
 * Verifies that a written sequence respects all scenario checks.
 * One ECHEC per failed check; score computed from failure count.
 * Failures are NOT fed back to the Writer (checks are enforced via constraints).
 * TODO: add dual representation (verification text vs Writer instruction) to enable Writer feedback.
 */
public class CheckCritic implements Agent {

    private static final String SYSTEM = """
            Tu es un editeur qui verifie qu'un texte respecte une liste de points de controle.
            Examine chaque point individuellement.
            Pour chaque point non respecte dans le texte, ecris une ligne :
            ECHEC: [le point tel quel]
            Si tous les points sont respectes : ecrire TOUT_OK — rien d'autre.
            Rien d'autre : ni introduction ni conclusion. En francais.""";

    private static final String AGENT_NAME = "SequenceCheckCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public CheckCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public CheckCriticOutput call(CheckCriticInput input) {
        String checksBlock = input.checks().stream()
                .map(c -> "- " + c)
                .collect(Collectors.joining("\n"));
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Points de contrôle", checksBlock)
                .section("Texte", t.text(input.sequenceText(), llm.contextWindow() * 4 / 2, "sequenceText"))
                .raw("Vérifie que chaque point est respecté dans le texte.")
                .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> failures = parseFailures(raw);
        int score = Math.max(1, 10 - failures.size() * 3);
        return new CheckCriticOutput(failures, score);
    }

    private static List<String> parseFailures(String response) {
        List<String> failures = new ArrayList<>();
        if (response == null || response.trim().startsWith("TOUT_OK")) return failures;
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (t.startsWith("ECHEC:")) {
                String f = t.substring("ECHEC:".length()).trim();
                if (!f.isBlank()) failures.add(f);
            }
        }
        return failures;
    }
}
