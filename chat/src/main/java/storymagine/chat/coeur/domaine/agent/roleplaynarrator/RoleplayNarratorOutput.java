package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

/**
 * replyText has the "[NEXT ACT]" marker already stripped ; triggeredNextAct reports whether it
 * was present. thinking is the model's reasoning text — empty unless the session opted in via
 * GenerationSettings.showThinking (see RoleplayNarrator).
 */
public record RoleplayNarratorOutput(String replyText, boolean triggeredNextAct, String thinking) {
}
