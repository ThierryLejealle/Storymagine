package storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacritic;

import java.util.List;

/** Output of DeusInMachinaCritic — detected instruction leaks in the prose. */
public record DeusInMachinaCriticOutput(List<String> leaks) {
    public boolean hasLeaks() { return !leaks.isEmpty(); }
    public String summary() {
        return hasLeaks() ? leaks.size() + " fuite(s) : " + String.join(" | ", leaks) : "OK";
    }
    public int score() { return Math.max(1, 10 - Math.min(leaks.size() * 3, 9)); }
}
