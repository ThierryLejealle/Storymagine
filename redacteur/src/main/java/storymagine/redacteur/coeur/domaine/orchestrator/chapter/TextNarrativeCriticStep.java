package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.textnarrativecritic.TextNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textnarrativecritic.TextNarrativeCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.textnarrativecritic.TextNarrativeCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates TextNarrativeCritic on a chapter text (IMPERATIVE chapters). */
public class TextNarrativeCriticStep {

    private final TextNarrativeCritic agent;

    public TextNarrativeCriticStep(TextNarrativeCritic agent) {
        this.agent = agent;
    }

    public TextNarrativeCriticOutput run(String text, Scenario scenario) {
        return agent.call(new TextNarrativeCriticInput(
                text,
                ScenarioFormatters.bookGoal(scenario)
        ));
    }
}
