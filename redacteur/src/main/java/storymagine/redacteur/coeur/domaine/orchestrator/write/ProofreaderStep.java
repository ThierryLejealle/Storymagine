package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.proofreader.Proofreader;
import storymagine.redacteur.coeur.domaine.agent.writer.proofreader.ProofreaderInput;
import storymagine.redacteur.coeur.domaine.agent.writer.proofreader.ProofreaderOutput;

/** Activates Proofreader on a sequence text. */
public class ProofreaderStep {

    private final Proofreader agent;

    public ProofreaderStep(Proofreader agent) {
        this.agent = agent;
    }

    public ProofreaderOutput run(String text) {
        return agent.call(new ProofreaderInput(text));
    }
}
