package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilter;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilterInput;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilterOutput;

import java.util.List;

/** Activates RepetitionFilter to remove intentional leitmotivs from the candidate ban list. */
public class RepetitionFilterStep {

    private final RepetitionFilter agent;

    public RepetitionFilterStep(RepetitionFilter agent) {
        this.agent = agent;
    }

    public RepetitionFilterOutput run(List<String> candidates, String keepPhrasesContent) {
        return agent.call(new RepetitionFilterInput(candidates, keepPhrasesContent));
    }
}
