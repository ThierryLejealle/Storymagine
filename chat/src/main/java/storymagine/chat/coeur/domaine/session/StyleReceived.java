package storymagine.chat.coeur.domaine.session;

/**
 * The style advice a character actually received for its last reply, once the {N%} lines were drawn
 * (see ScenarioStyle.drawn) : text is empty when every line was dropped. Debug only, kept in memory.
 */
public record StyleReceived(String npcLabel, String text) {

    public StyleReceived {
        text = text == null ? "" : text;
    }
}
