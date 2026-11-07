package storymagine.chat.ui.web;

/**
 * One Cast member's vignette state — id is what POST /cycle-activity expects back. level is the
 * activity in force (active, present, passive or absent), source where it comes from (implicit,
 * explicit or forced by the player) and scenarioLevel what the scenario alone gives, for the tooltip.
 * doMode is true while the character holds the narrator's pen (DO mode, see NpcMode) : a star on its vignette.
 * publicSheet is the public part of its sheet at the current act (tags and deltas applied, never the "# SECRET"
 * part), shown in the popup over the vignette.
 */
public record NpcView(String id, String label, String level, String source, String scenarioLevel, boolean doMode,
                      String publicSheet) {
}
