package storymagine.chat.coeur.domaine.agent.replyorigin;

/**
 * traced lists the elements of the reply that have a matching passage in the prompt (one line each,
 * with the block, a short quote and whether the link is direct or indirect) ; untraced lists those
 * with no matching passage ; reading is a short summary of which blocks weigh most. Lines of traced
 * and untraced are separated by a line break ; "[RIEN]" from the model becomes the empty string.
 */
public record ReplyOriginAnalystOutput(String traced, String untraced, String reading) {
}
