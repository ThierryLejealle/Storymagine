package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.naturalityfilter.NaturalityFilter;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalityfilter.NaturalityFilterInput;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalityfilter.NaturalityFilterOutput;

/** Activates NaturalityFilter on the full chapter text (all chapter types, SIMPLE and FULL quality). */
public class NaturalityFilterStep {

    private final NaturalityFilter agent;

    public NaturalityFilterStep(NaturalityFilter agent) {
        this.agent = agent;
    }

    public NaturalityFilterOutput run(String text) {
        return agent.call(new NaturalityFilterInput(text));
    }
}
