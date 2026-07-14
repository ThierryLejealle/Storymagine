package storymagine.chat.coeur.domaine.agent.npcmindstate;

/**
 * situation restates what is currently happening from the character's point of view ; thoughts is
 * their private honest reaction (never spoken aloud) ; plans is what they currently intend to do
 * next ("[RIEN]" from the model becomes the empty string here — see NpcMindStateAnalyst).
 */
public record NpcMindStateAnalystOutput(String situation, String thoughts, String plans) {
}
