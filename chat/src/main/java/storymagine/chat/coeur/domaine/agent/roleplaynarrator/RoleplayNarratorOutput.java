package storymagine.chat.coeur.domaine.agent.roleplaynarrator;

import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;

/**
 * replyText has the "[NEXT ACT]" marker already stripped ; triggeredNextAct reports whether it
 * was present. thinking is the model's reasoning text — empty unless the session opted in via
 * GenerationSettings.showThinking (see RoleplayNarrator). prompt is the exact system + user text sent to
 * the model for this reply (see ReplyOriginAnalyst) ; null when unknown.
 */
public record RoleplayNarratorOutput(String replyText, boolean triggeredNextAct, String thinking,
                                     ChatPrompt prompt) {

    /** Without the prompt (tests, callers that never look back at it). */
    public RoleplayNarratorOutput(String replyText, boolean triggeredNextAct, String thinking) {
        this(replyText, triggeredNextAct, thinking, null);
    }
}
