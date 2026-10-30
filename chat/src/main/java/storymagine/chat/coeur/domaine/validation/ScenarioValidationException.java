package storymagine.chat.coeur.domaine.validation;

import java.util.stream.Collectors;

/** Thrown when loading a scenario whose validation found errors : the message lists every one of them. */
public class ScenarioValidationException extends IllegalStateException {

    private final ScenarioReport report;

    public ScenarioValidationException(ScenarioReport report) {
        super("Scenario invalide (" + report.errors().size() + " erreur(s)) :\n"
            + report.errors().stream().map(i -> "  " + i.display()).collect(Collectors.joining("\n")));
        this.report = report;
    }

    public ScenarioReport report() {
        return report;
    }
}
