package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker.SequenceChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker.SequenceCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker.SequenceCheckerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;

/** Activates SequenceChecker to verify all required elements are present in the written text. */
public class SequenceCheckerStep {

    private final SequenceChecker agent;

    public SequenceCheckerStep(SequenceChecker agent) {
        this.agent = agent;
    }

    public SequenceCheckerOutput run(Sequence sequence, String sequenceText,
                                     Scenario scenario, Chapter chapter) {
        return agent.call(new SequenceCheckerInput(
                sequence.directive(),
                sequenceText,
                ScenarioFormatters.writerChecks(scenario, chapter, sequence)
        ));
    }
}
