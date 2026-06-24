package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker.SequenceStyleChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker.SequenceStyleCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker.SequenceStyleCheckerOutput;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates SequenceStyleChecker to evaluate style quality of a written sequence. */
public class SequenceStyleCheckerStep {

    private final SequenceStyleChecker agent;

    public SequenceStyleCheckerStep(SequenceStyleChecker agent) {
        this.agent = agent;
    }

    public SequenceStyleCheckerOutput run(String text, Scenario scenario) {
        return agent.call(new SequenceStyleCheckerInput(
                text,
                null,
                null,
                scenario.writingExample()
        ));
    }
}
