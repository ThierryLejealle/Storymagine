package storymagine.chat.ui.web;

/**
 * One Cast member's vignette state — id is what POST /set-present and POST /set-interjecting
 * expect back. interjecting is whether this Npc currently opts in to unprompted reactions (see
 * SpeakerSelector.rollInterjectors).
 */
public record NpcView(String id, String label, boolean present, boolean interjecting) {
}
