package storymagine.chat.coeur.service;

import java.util.List;

/**
 * Debug view : the scenario part of what a character's prompt contains, exactly as
 * ChatPromptBuilder inserts it — the premise, then the current act's text (a leaf's text already
 * holds every ancestor heading's body, root to leaf). actText is empty when no act is active.
 * style is the style advice sent after STORY SO FAR ("" when there is none).
 * warnings are the validation warnings of the scenario (see ScenarioValidator.checkScenario).
 */
public record ScenarioPromptView(String premise, int currentAct, int totalActs, String actText, String style,
                                 List<String> warnings) {}
