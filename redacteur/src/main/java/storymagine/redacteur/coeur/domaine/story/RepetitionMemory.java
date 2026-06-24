package storymagine.redacteur.coeur.domaine.story;

import java.util.ArrayList;
import java.util.List;

/**
 * Sliding-window blacklist of phrases and themes to avoid repetition across sequences.
 * Window sizes are calibrated to the model's context window at session start.
 */
public class RepetitionMemory {

    private final List<String> forbiddenPhrases = new ArrayList<>();
    private final List<String> forbiddenThemes  = new ArrayList<>();
    private int maxPhrases = 20;
    private int maxThemes  = 8;

    /** Calibrates window sizes based on the model's context window (in tokens). */
    public void calibrate(int contextWindow) {
        maxPhrases = Math.min(60, Math.max(10, contextWindow / 200));
        maxThemes  = Math.min(10, Math.max(4,  contextWindow / 400));
    }

    public void addPhrases(List<String> phrases) {
        forbiddenPhrases.addAll(phrases);
        while (forbiddenPhrases.size() > maxPhrases) forbiddenPhrases.remove(0);
    }

    public void addThemes(List<String> themes) {
        forbiddenThemes.addAll(themes);
        while (forbiddenThemes.size() > maxThemes) forbiddenThemes.remove(0);
    }

    public List<String> forbiddenPhrases() { return List.copyOf(forbiddenPhrases); }
    public List<String> forbiddenThemes()  { return List.copyOf(forbiddenThemes); }
}
