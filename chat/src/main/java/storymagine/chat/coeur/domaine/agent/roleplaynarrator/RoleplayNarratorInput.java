package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.Scene;

import java.util.List;

/**
 * scene says who is actually speaking this turn (and who else is present) — see ChatServiceImpl,
 * which runs SpeakerSelector before building this. recentTurns already ends with the player's own
 * line (appended to the session before this agent is called) — no separate "current player
 * message" field : ChatPromptBuilder reads it straight out of the last entry of recentTurns.
 */
public record RoleplayNarratorInput(ChatScenario scenario, Scene scene, int currentAct, String summary,
                                     List<ChatTurn> recentTurns, GenerationSettings settings) {
}
