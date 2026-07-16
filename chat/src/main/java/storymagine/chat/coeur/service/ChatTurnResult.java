package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.session.ChatTurn;

import java.util.ArrayList;
import java.util.List;

/**
 * Outcome of one exchange : the player's turn plus every reply turn produced this round (one per
 * Npc SpeakerSelector picked — see ChatServiceImpl.generateRepliesAndFinish, usually one or two),
 * whether the history got compacted this time, an estimated size (chars/4, see ChatContextBudget)
 * of the prompt the NEXT turn would send given the session's state right now — after this turn's
 * append and any compaction, so the UI's context-usage gauge reflects reality immediately rather
 * than what Ollama measured for the call that just happened — whether any reply triggered the next
 * act via the "[NEXT ACT]" marker, and the NARRATOR beat turns that advancing the act just
 * appended (empty if it didn't). replacedTurnCount is how many already-displayed trailing turns
 * the UI must remove before appending newTurns() : 0 for a normal exchange (sendMessage), the size
 * of the whole regenerated round for retry(). See newTurns() : the UI appends these to what it
 * already shows instead of repainting from the session's current (possibly just-compacted, hence
 * shorter) turn window.
 */
public record ChatTurnResult(ChatTurn playerTurn, List<ChatTurn> replyTurns, boolean compacted, int promptTokens,
                              boolean actAdvanced, List<ChatTurn> narratorTurnsFromActAdvance,
                              int replacedTurnCount) {

    /** Every turn this exchange actually added, in order — what the UI should append, verbatim. */
    public List<ChatTurn> newTurns() {
        List<ChatTurn> all = new ArrayList<>();
        all.add(playerTurn);
        all.addAll(replyTurns);
        all.addAll(narratorTurnsFromActAdvance);
        return all;
    }
}
