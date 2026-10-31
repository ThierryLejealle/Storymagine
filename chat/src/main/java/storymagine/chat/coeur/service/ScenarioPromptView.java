package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.session.StyleReceived;

import java.util.List;

/**
 * Debug view : the scenario part of what a character's prompt contains, exactly as
 * ChatPromptBuilder inserts it — the premise, then the current act's text (a leaf's text already
 * holds every ancestor heading's body, root to leaf). actText is empty when no act is active.
 * style is the style advice as written in style.txt ("" when there is none) ; when it holds {N%}
 * lines, styleReceived is what the last reply actually received once they were drawn (null before
 * any reply).
 * warnings are the validation warnings of the scenario (see ScenarioValidator.checkScenario).
 */
public record ScenarioPromptView(String premise, int currentAct, int totalActs, String actText, String style,
                                 StyleReceived styleReceived, List<String> warnings) {

    public ScenarioPromptView(String premise, int currentAct, int totalActs, String actText, String style,
                              List<String> warnings) {
        this(premise, currentAct, totalActs, actText, style, null, warnings);
    }
}
