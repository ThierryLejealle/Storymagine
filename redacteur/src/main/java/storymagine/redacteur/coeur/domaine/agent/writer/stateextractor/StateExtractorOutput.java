package storymagine.redacteur.coeur.domaine.agent.writer.stateextractor;

/**
 * Output of StateExtractor — ETAT/EVENT lines, or "AUCUN".
 * Parsed by the service layer and accumulated into story memory.
 */
public record StateExtractorOutput(String stateLines) {
    public boolean hasChanges() {
        return stateLines != null && !stateLines.isBlank() && !stateLines.equalsIgnoreCase("AUCUN");
    }
}
