package storymagine.chat.coeur.domaine.agent.replyorigin;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.GenerationSettings;
import storymagine.chat.coeur.domaine.session.ReplyTrace;

/** The scenario (its name and language), the reply to look back at with the prompt that produced it, and the session's settings. */
public record ReplyOriginAnalystInput(ChatScenario scenario, ReplyTrace trace, GenerationSettings settings) {
}
