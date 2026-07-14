package storymagine.chat.coeur.domaine.agent.npcmindstate;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;

import java.util.List;

/** Same shape as RoleplayNarratorInput, minus the player's new line — there is none, this is a read-only check. */
public record NpcMindStateAnalystInput(ChatScenario scenario, int currentAct, String summary,
                                        List<ChatTurn> recentTurns, GenerationSettings settings) {
}
