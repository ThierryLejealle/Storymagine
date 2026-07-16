package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewer;
import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewerInput;
import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewerOutput;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewer;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewerInput;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewerOutput;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenariotester.ActTestResult;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;
import storymagine.commun.coeur.ports.LogPort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Dry-runs a ChatScenario's acts through ScenarioContinuityReviewer (cumulative story-so-far) and
 * ScenarioClarityReviewer (each act in isolation), act by act, in play order. Merges both agents'
 * suggestions per act — no LLM synthesis call, no score : see ScenarioTestReport.
 */
public class ScenarioTesterServiceImpl implements ScenarioTesterService {

    private final ScenarioContinuityReviewer continuityReviewer;
    private final ScenarioClarityReviewer     clarityReviewer;
    private final LogPort                     log;

    public ScenarioTesterServiceImpl(ScenarioContinuityReviewer continuityReviewer,
                                      ScenarioClarityReviewer clarityReviewer, LogPort log) {
        this.continuityReviewer = continuityReviewer;
        this.clarityReviewer    = clarityReviewer;
        this.log                = log;
    }

    @Override
    public ScenarioTestReport testScenario(ChatScenario scenario) {
        List<ActTestResult> results     = new ArrayList<>();
        StringBuilder        storySoFar = new StringBuilder();
        // Teste avec la fiche du premier Npc du Cast — un choix simple, deterministe : cet outil
        // de QA statique (hors session de jeu) n'a pas de notion de "qui parle ce tour" a lui
        // donner, contrairement a RoleplayNarrator (voir SpeakerSelector/Scene).
        String characterSheet = scenario.cast().npcs().get(0).fullSheet();

        for (ScenarioAct act : scenario.acts()) {
            ScenarioContinuityReviewerOutput continuity = continuityReviewer.call(new ScenarioContinuityReviewerInput(
                characterSheet, scenario.premise(), storySoFar.toString(),
                act.number(), act.title(), act.text()));

            ScenarioClarityReviewerOutput clarity = clarityReviewer.call(new ScenarioClarityReviewerInput(
                characterSheet, scenario.premise(), act.number(), act.title(), act.text()));

            List<String> suggestions = Stream.concat(
                    continuity.suggestions().stream().map(s -> "[Continuité] " + s),
                    clarity.suggestions().stream().map(s -> "[Clarté] " + s))
                .toList();
            results.add(new ActTestResult(act.number(), act.title(), continuity.issues(), clarity.issues(), suggestions));

            log.info("Acte " + act.number().display() + " testé : " + continuity.issues().size()
                + " incohérence(s), " + clarity.issues().size() + " problème(s) de clarté, "
                + suggestions.size() + " suggestion(s).");

            storySoFar.append("Acte ").append(act.number().display()).append(" — ").append(act.title())
                .append(" :\n").append(act.text()).append("\n\n");
        }

        return new ScenarioTestReport(scenario.name(), results);
    }
}
