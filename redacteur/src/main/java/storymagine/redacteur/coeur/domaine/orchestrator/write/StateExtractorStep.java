package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractor;
import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractorInput;
import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractorOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.story.WorldState;

/** Activates StateExtractor on a sequence text to detect entity and event changes. */
public class StateExtractorStep {

    private final StateExtractor agent;

    public StateExtractorStep(StateExtractor agent) {
        this.agent = agent;
    }

    public StateExtractorOutput run(String sequenceText, WorldState worldState) {
        return agent.call(new StateExtractorInput(
                sequenceText,
                StoryFormatters.entityState(worldState)
        ));
    }
}
