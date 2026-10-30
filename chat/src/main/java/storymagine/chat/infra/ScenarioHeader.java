package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The header of scenario.txt : the consecutive "Joueur : X" / "Langue : X" lines (any order) at the
 * very top ; the first other line, blank included, ends it. rest is everything after the header,
 * headerLines how many lines it took (see ChatFileStorageAdapter).
 */
record ScenarioHeader(String playerName, ScenarioLanguage language, String rest, int headerLines) {

    private static final Pattern PLAYER_NAME_LINE = Pattern.compile("^Joueur\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LANGUAGE_LINE    = Pattern.compile("^Langue\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    static ScenarioHeader parse(String raw) {
        String[] lines = raw.split("\n", -1);
        String player = null;
        ScenarioLanguage language = ScenarioLanguage.UNSPECIFIED;
        int i = 0;
        for (; i < lines.length; i++) {
            String line = lines[i].strip();
            Matcher playerLine = PLAYER_NAME_LINE.matcher(line);
            Matcher languageLine = LANGUAGE_LINE.matcher(line);
            if (playerLine.matches()) player = playerLine.group(1).strip();
            else if (languageLine.matches()) language = ScenarioLanguage.parse(languageLine.group(1));
            else break;
        }
        return new ScenarioHeader(player, language, String.join("\n", Arrays.asList(lines).subList(i, lines.length)), i);
    }
}
