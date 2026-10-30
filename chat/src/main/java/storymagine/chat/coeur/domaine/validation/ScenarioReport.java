package storymagine.chat.coeur.domaine.validation;

import java.util.List;

/** Everything a validation found, in the order met. */
public record ScenarioReport(List<ScenarioIssue> issues) {

    public static final ScenarioReport CLEAN = new ScenarioReport(List.of());

    public ScenarioReport {
        issues = List.copyOf(issues);
    }

    public List<ScenarioIssue> errors() {
        return issues.stream().filter(i -> i.severity() == ScenarioIssue.Severity.ERROR).toList();
    }

    public List<ScenarioIssue> warnings() {
        return issues.stream().filter(i -> i.severity() == ScenarioIssue.Severity.WARNING).toList();
    }

    public boolean hasErrors() {
        return !errors().isEmpty();
    }
}
