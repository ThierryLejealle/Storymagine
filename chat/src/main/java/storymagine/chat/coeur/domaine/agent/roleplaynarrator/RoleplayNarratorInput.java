package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.PlayerMessage;

import java.util.List;

public record RoleplayNarratorInput(ChatScenario scenario, int currentAct, String summary,
                                     List<ChatTurn> recentTurns, PlayerMessage playerMessage,
                                     GenerationSettings settings) {
}
