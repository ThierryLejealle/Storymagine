package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.textdreamcritic.TextDreamCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textdreamcritic.TextDreamCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.textdreamcritic.TextDreamCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates TextDreamCritic on a DREAM chapter text. */
public class TextDreamCriticStep {

    private final TextDreamCritic agent;

    public TextDreamCriticStep(TextDreamCritic agent) {
        this.agent = agent;
    }

    public TextDreamCriticOutput run(String text, Scenario scenario) {
        return agent.call(new TextDreamCriticInput(
                text,
                ScenarioFormatters.bookGoal(scenario),
                "moyen"
        ));
    }
}
