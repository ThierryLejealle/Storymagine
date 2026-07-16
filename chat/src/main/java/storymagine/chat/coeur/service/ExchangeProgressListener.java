package storymagine.chat.coeur.service;

import storymagine.chat.coeur.domaine.session.ChatTurn;

/**
 * Progress callback for a streaming exchange (see ChatService.sendMessage) : onPartialReply fires
 * repeatedly, in order, as an Npc's reply text grows — always the FULL text generated so far for
 * that Npc, not just the newest fragment, so a caller can just replace what it's showing instead of
 * tracking an accumulator itself (also makes a rare mid-generation retry, see OllamaAdapter,
 * harmless : the next call simply reports a shorter text again). onTurnReady fires once per turn
 * this exchange appends to the session (the player's own turn first, then each Npc reply in order,
 * then any NARRATOR beat turns an act advance triggers) — exactly the moment that turn is final.
 */
public interface ExchangeProgressListener {

    ExchangeProgressListener NOOP = new ExchangeProgressListener() {
        @Override public void onPartialReply(String npcId, String textSoFar) {}
        @Override public void onTurnReady(ChatTurn turn) {}
    };

    void onPartialReply(String npcId, String textSoFar);

    void onTurnReady(ChatTurn turn);
}
