package storymagine.chat.coeur.domaine.validation;

/**
 * One problem found in a scenario : where (file and line, or an act) and what. An ERROR makes the
 * scenario unusable (loading is refused), a WARNING is shown but does not block.
 */
public record ScenarioIssue(Severity severity, String where, String message) {

    public enum Severity { ERROR, WARNING }

    public static ScenarioIssue error(String where, String message) {
        return new ScenarioIssue(Severity.ERROR, where, message);
    }

    public static ScenarioIssue warning(String where, String message) {
        return new ScenarioIssue(Severity.WARNING, where, message);
    }

    /** "ERREUR        scenario.txt:12 : ..." — severity padded so the locations line up in a list. */
    public String display() {
        String label = severity == Severity.ERROR ? "ERREUR" : "AVERTISSEMENT";
        return String.format("%-13s %s : %s", label, where, message);
    }
}
