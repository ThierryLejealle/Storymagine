package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTracker;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTrackerInput;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTrackerOutput;
import storymagine.redacteur.coeur.domaine.story.RepetitionMemory;

/** Activates RepetitionTracker to extract new forbidden phrases and themes from a sequence. */
public class RepetitionTrackerStep {

    private final RepetitionTracker agent;

    public RepetitionTrackerStep(RepetitionTracker agent) {
        this.agent = agent;
    }

    public RepetitionTrackerOutput run(String sequenceText, RepetitionMemory memory) {
        return agent.call(new RepetitionTrackerInput(
                sequenceText,
                memory.forbiddenPhrases(),
                memory.forbiddenThemes()
        ));
    }
}
