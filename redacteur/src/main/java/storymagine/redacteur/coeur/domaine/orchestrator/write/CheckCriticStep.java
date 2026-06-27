package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic.CheckCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic.CheckCriticInput;
import storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic.CheckCriticOutput;

import java.util.List;

/** Activates CheckCritic to verify scenario checks are respected in the written text. */
public class CheckCriticStep {

    private final CheckCritic agent;

    public CheckCriticStep(CheckCritic agent) {
        this.agent = agent;
    }

    public CheckCriticOutput run(String sequenceText, List<String> checks) {
        return agent.call(new CheckCriticInput(checks, sequenceText));
    }
}
