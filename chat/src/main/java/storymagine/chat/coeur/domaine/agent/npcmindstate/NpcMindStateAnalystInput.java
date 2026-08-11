package storymagine.chat.coeur.domaine.agent.npcmindstate;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;

import java.util.List;

/**
 * Same shape as RoleplayNarratorInput, minus the player's new line — there is none, this is a
 * read-only check. speaker is the Npc the player picked to inspect (see ChatServiceImpl.analyzeMindState).
 */
public record NpcMindStateAnalystInput(ChatScenario scenario, Npc speaker, int currentAct, String summary,
                                        List<ChatTurn> recentTurns, GenerationSettings settings) {
}
