package storymagine.redacteur.coeur.domaine.orchestrator.evaluate;

import storymagine.redacteur.coeur.domaine.agent.global.chapterstylechecker.ChapterStyleChecker;
import storymagine.redacteur.coeur.domaine.agent.global.chapterstylechecker.ChapterStyleCheckerInput;
import storymagine.redacteur.coeur.domaine.agent.global.chapterstylechecker.ChapterStyleCheckerOutput;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.story.Story;

/** Activates ChapterStyleChecker on the full chapter text. */
public class ChapterStyleCheckerStep {

    private final ChapterStyleChecker agent;

    public ChapterStyleCheckerStep(ChapterStyleChecker agent) {
        this.agent = agent;
    }

    public ChapterStyleCheckerOutput run(Story story, Scenario scenario) {
        String text = story.currentChapter().orElseThrow().fullText();
        return agent.call(new ChapterStyleCheckerInput(
                text,
                null,
                null,
                scenario.writingExample()
        ));
    }
}
