package storymagine.redacteur.coeur.domaine.orchestrator.chapter;

import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.TextCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.TextCoherenceCriticInput;
import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.TextCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;

/** Activates TextCoherenceCritic on a chapter text (IMPERATIVE and WHAT_IF chapters). */
public class TextCoherenceCriticStep {

    private final TextCoherenceCritic agent;

    public TextCoherenceCriticStep(TextCoherenceCritic agent) {
        this.agent = agent;
    }

    public TextCoherenceCriticOutput run(String text, Scenario scenario, Chapter chapter) {
        return agent.call(new TextCoherenceCriticInput(
                text,
                String.join("\n", ScenarioFormatters.writerChecks(scenario, chapter)),
                ScenarioFormatters.writerConstraints(scenario, chapter),
                ScenarioFormatters.focusText(chapter.defaults().focus(), true)
        ));
    }
}
