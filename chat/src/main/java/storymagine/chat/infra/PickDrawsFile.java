package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.pick.PickDraw;
import storymagine.chat.coeur.domaine.pick.PickDraws;
import storymagine.chat.coeur.domaine.pick.PickMode;
import storymagine.chat.coeur.domaine.pick.PickTag;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text format of run/current/resolved/picks.txt : one block per "{pick ...}" command, a header line
 * "@@ unique place" (mode, library) followed by the paragraphs as drawn, separated by a line
 * "@@ --" when the command handed out several. A paragraph line starting with "@@ " would be read
 * as a header — not something a picks/ library holds.
 */
final class PickDrawsFile {

    private static final Pattern HEADER = Pattern.compile("@@ (\\S+) (\\S+)");
    private static final String NEXT_PARAGRAPH = "@@ --";

    private PickDrawsFile() {}

    static String format(PickDraws draws) {
        StringBuilder sb = new StringBuilder();
        for (PickDraw draw : draws.values()) {
            sb.append("@@ ").append(draw.mode().label()).append(' ').append(draw.tag()).append('\n');
            for (int i = 0; i < draw.paragraphs().size(); i++) {
                if (i > 0) sb.append(NEXT_PARAGRAPH).append('\n');
                sb.append(draw.paragraphs().get(i)).append('\n');
            }
        }
        return sb.toString();
    }

    static PickDraws parse(String content) {
        List<PickDraw> draws = new ArrayList<>();
        PickMode mode = null;
        PickTag tag = null;
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : content.replace("\r\n", "\n").split("\n", -1)) {
            Matcher header = HEADER.matcher(line);
            if (line.equals(NEXT_PARAGRAPH) && mode != null) {
                paragraphs.add(current.toString().strip());
                current.setLength(0);
            } else if (header.matches()) {
                if (mode != null) {
                    paragraphs.add(current.toString().strip());
                    draws.add(new PickDraw(mode, tag, paragraphs));
                }
                mode = PickMode.parse(header.group(1));
                tag = new PickTag(header.group(2));
                paragraphs = new ArrayList<>();
                current.setLength(0);
            } else if (mode != null) {
                current.append(line).append('\n');
            }
        }
        if (mode != null) {
            paragraphs.add(current.toString().strip());
            draws.add(new PickDraw(mode, tag, paragraphs));
        }
        return new PickDraws(draws);
    }
}
