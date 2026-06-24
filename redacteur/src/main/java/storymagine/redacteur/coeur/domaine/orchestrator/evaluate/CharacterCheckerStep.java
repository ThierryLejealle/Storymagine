package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.redacteur.coeur.domaine.agent.global.characterchecker.CharacterChecker;
import storymagine.redacteur.coeur.domaine.agent.global.characterchecker.CharacterCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.global.characterchecker.CharacterCheckerOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates CharacterChecker on the full chapter text. */
public class CharacterCheckerStep {

    private final CharacterChecker agent;

    public CharacterCheckerStep(CharacterChecker agent) {
        this.agent = agent;
    }

    public CharacterCheckerOutput run(Story story, Scenario scenario, Chapter chapter) {
        String text = story.currentChapter().orElseThrow().fullText();
        return agent.call(new CharacterCheckerInput(
                text,
                ScenarioFormatters.personnages(chapter.defaults().characters(), false)
        ));
    }
}
