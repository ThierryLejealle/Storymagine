package storymagine.chat.coeur.domaine.session;

/**
 * What it takes to look back at one reply, kept in memory only (never persisted) : the exact prompt
 * the character was given (system and user parts, as sent to the model), the reply it produced and
 * its reasoning when the session showed it. Used by ReplyOriginAnalyst, see ChatSession.lastReplyTrace.
 */
public record ReplyTrace(String npcId, String system, String user, String reply, String thinking) {

    public ReplyTrace {
        thinking = thinking == null ? "" : thinking;
    }
}
