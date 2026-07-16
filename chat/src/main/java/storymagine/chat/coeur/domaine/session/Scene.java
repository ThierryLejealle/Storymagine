package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.List;

/**
 * Who is speaking this turn, and who else is present in the story right now — assembled by
 * ChatServiceImpl from SpeakerSelector's choice plus the session's present Npcs, consumed by
 * RoleplayNarrator so it can acknowledge the rest of the cast without receiving their full sheets
 * (only otherPresent's names are meant to reach the prompt, never their characterSheet).
 * interjecting is true when speaker was not the one addressed this turn but is reacting anyway
 * (see SpeakerSelector.rollInterjectors) — tells ChatPromptBuilder to add the shorter-reaction
 * rule instead of treating this like a normal, fully-addressed turn.
 */
public record Scene(Npc speaker, List<Npc> otherPresent, boolean interjecting) {
}
