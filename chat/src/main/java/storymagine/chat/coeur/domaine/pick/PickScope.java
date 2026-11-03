package storymagine.chat.coeur.domaine.pick;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The copy of a "{repeat}" block a draw was made in : block is the block's number in the scenario (1, 2...,
 * so two blocks never mix), label what the block repeats over (the library, the variable of a cast, or
 * "repeat" for "{repeat N}"), copy its number among copies. NONE for a draw outside any block. token() is
 * the one-word form written in picks.txt and in the internal copy-start line, display() the one shown to
 * the author ("defis 2/3").
 */
public record PickScope(int block, String label, int copy, int copies) {

    public static final PickScope NONE = new PickScope(0, "", 0, 0);
    private static final Pattern TOKEN = Pattern.compile("(\\d+):([^:\\s]+):(\\d+)/(\\d+)");

    public boolean isNone() {
        return block == 0;
    }

    public String token() {
        return block + ":" + label + ":" + copy + "/" + copies;
    }

    public String display() {
        return isNone() ? "" : label + " " + copy + "/" + copies;
    }

    /** The scope a token stands for ; empty when the text is not a token. */
    public static Optional<PickScope> parse(String token) {
        if (token == null) return Optional.empty();
        Matcher m = TOKEN.matcher(token);
        if (!m.matches()) return Optional.empty();
        return Optional.of(new PickScope(Integer.parseInt(m.group(1)), m.group(2), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4))));
    }
}
