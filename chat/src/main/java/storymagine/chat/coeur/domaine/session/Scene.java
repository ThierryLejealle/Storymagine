package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.List;

/**
 * Who is speaking this turn, who else is present in the story right now, and which teammates are
 * absent from THIS scene (muted, see ChatSession.setPresent) but still part of the cast — assembled
 * by ChatServiceImpl from SpeakerSelector's choice plus the session's present Npcs, consumed by
 * RoleplayNarrator/ChatPromptBuilder. speaker gets their own full sheet ; otherPresent and
 * absentTeammates both get their PUBLIC sheet only (never a secret, see ChatPromptBuilder) — an
 * absent teammate hasn't vanished from the world, they're just not physically in this scene, so the
 * speaker still knows them, just can't have them act or speak. interjecting is true when speaker
 * was not the one addressed this turn but is reacting anyway (see SpeakerSelector.rollInterjectors)
 * — tells ChatPromptBuilder to add the shorter-reaction rule instead of treating this like a
 * normal, fully-addressed turn.
 */
public record Scene(Npc speaker, List<Npc> otherPresent, List<Npc> absentTeammates, boolean interjecting) {
}
