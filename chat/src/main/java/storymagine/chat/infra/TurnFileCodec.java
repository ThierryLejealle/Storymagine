package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.NumberedTurn;
import storymagine.chat.coeur.domaine.session.TurnNumber;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text format of one turn file : a separator line carrying the turn's number and the act in force
 * when it was written, the speaker header, then the turn's text verbatim.
 * <pre>
 * ----- #13 · acte 2 -----
 * ### LLM: elena
 * text of the turn
 * </pre>
 * Concatenated in order, the files read as one journal. The separator exists only on disk : the
 * number is a field of NumberedTurn, never part of ChatTurn.text(), so the model never reads it.
 */
final class TurnFileCodec {

    private static final Pattern SEPARATOR = Pattern.compile("^----- #(\\d+) · acte (\\d+) -----$");

    private static final String PLAYER_HEADER   = "### PLAYER";
    private static final String LLM_HEADER      = "### LLM";
    private static final String NARRATOR_HEADER = "### NARRATOR";

    private TurnFileCodec() {}

    static String format(NumberedTurn numbered) {
        ChatTurn turn = numbered.turn();
        return "----- #" + numbered.number().value() + " · acte " + numbered.act() + " -----\n"
            + headerFor(turn) + "\n" + turn.text() + "\n";
    }

    /** LLM turns carry their npcId as "### LLM: {id}" ; PLAYER/NARRATOR never have one to carry. */
    private static String headerFor(ChatTurn turn) {
        return switch (turn.speaker()) {
            case PLAYER   -> PLAYER_HEADER;
            case LLM      -> turn.npcId() != null ? LLM_HEADER + ": " + turn.npcId() : LLM_HEADER;
            case NARRATOR -> NARRATOR_HEADER;
        };
    }

    /** source only labels the error message — the file the content was read from. */
    static NumberedTurn parse(String content, Object source) {
        String[] lines = content.replace("\r\n", "\n").split("\n", 3);
        Matcher separator = SEPARATOR.matcher(lines[0].strip());
        if (lines.length < 2 || !separator.matches()) {
            throw new IllegalStateException("Fichier de tour illisible (separateur \"----- #N · acte A -----\" attendu) : " + source);
        }
        String header = lines[1].strip();
        String text = lines.length > 2 ? lines[2].strip() : "";
        return new NumberedTurn(
            TurnNumber.of(Integer.parseInt(separator.group(1))),
            Integer.parseInt(separator.group(2)),
            toTurn(header, text, source));
    }

    private static ChatTurn toTurn(String header, String text, Object source) {
        if (header.equals(PLAYER_HEADER))   return new ChatTurn(ChatTurn.Speaker.PLAYER, text);
        if (header.equals(NARRATOR_HEADER)) return new ChatTurn(ChatTurn.Speaker.NARRATOR, text);
        if (header.equals(LLM_HEADER))      return new ChatTurn(ChatTurn.Speaker.LLM, text);
        if (header.startsWith(LLM_HEADER + ": ")) {
            return new ChatTurn(ChatTurn.Speaker.LLM, text, "", header.substring((LLM_HEADER + ": ").length()));
        }
        throw new IllegalStateException("En-tete de locuteur inconnu \"" + header + "\" : " + source);
    }
}
