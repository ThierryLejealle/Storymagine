package storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker;

import java.util.List;

/** Output of DeusInMachinaChecker — detected instruction leaks in the prose. */
public record DeusInMachinaCheckerOutput(List<String> leaks) {
    public boolean hasLeaks() { return !leaks.isEmpty(); }
    public String summary() {
        return hasLeaks() ? leaks.size() + " fuite(s) : " + String.join(" | ", leaks) : "OK";
    }
}
