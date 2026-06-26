package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector.StyleCorrector;
import storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector.StyleCorrectorInput;
import storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector.StyleCorrectorOutput;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates StyleCorrector to patch stylistic weaknesses inline. */
public class StyleCorrectorStep {

    private final StyleCorrector agent;

    public StyleCorrectorStep(StyleCorrector agent) {
        this.agent = agent;
    }

    public StyleCorrectorOutput run(String text, Scenario scenario) {
        return agent.call(new StyleCorrectorInput(
                text,
                null,
                null,
                scenario.writingExample()
        ));
    }
}
