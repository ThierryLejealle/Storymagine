package storymagine.chat.coeur.domaine.session;

/**
 * A ChatTurn together with what the run's history needs to remember about it : its TurnNumber and
 * the act that was current when it was written (0 for an act-less scenario). Kept apart from
 * ChatTurn so the number and the act never leak into the text the model reads — they only exist in
 * the persisted turn files and on the UI's bubbles.
 */
public record NumberedTurn(TurnNumber number, int act, ChatTurn turn) {
}
