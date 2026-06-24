package storymagine.redacteur.coeur.ports;

/** A validation error found in a scenario directory. */
public record ScenarioError(ScenarioErrorType type, String message) {

    public enum ScenarioErrorType {
        FILE_MISSING,
        INVALID_FORMAT,
        UNRESOLVED_REF
    }

    public static ScenarioError missingFile(String filename) {
        return new ScenarioError(ScenarioErrorType.FILE_MISSING, "Missing required file: " + filename);
    }

    public static ScenarioError invalidFormat(String filename, String detail) {
        return new ScenarioError(ScenarioErrorType.INVALID_FORMAT, filename + ": " + detail);
    }

    public static ScenarioError unresolvedRef(String ref, String context) {
        return new ScenarioError(ScenarioErrorType.UNRESOLVED_REF,
                "Unresolved reference '" + ref + "' in " + context);
    }
}
