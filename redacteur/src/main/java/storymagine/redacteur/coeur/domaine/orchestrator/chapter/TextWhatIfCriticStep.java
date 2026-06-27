package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.textwhatifcritic.TextWhatIfCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textwhatifcritic.TextWhatIfCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.textwhatifcritic.TextWhatIfCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates TextWhatIfCritic on a WHAT_IF chapter text. */
public class TextWhatIfCriticStep {

    private final TextWhatIfCritic agent;

    public TextWhatIfCriticStep(TextWhatIfCritic agent) {
        this.agent = agent;
    }

    public TextWhatIfCriticOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new TextWhatIfCriticInput(
                text,
                ScenarioFormatters.writerConstraints(scenario, chapter)
        ));
    }
}
