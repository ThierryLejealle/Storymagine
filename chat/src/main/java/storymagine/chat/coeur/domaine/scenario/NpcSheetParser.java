package storymagine.chat.coeur.domaine.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the text of a character sheet into an Npc. A line "# SECRET" (alone, any case) splits the sheet :
 * before it is public, after it secret ; with none the whole sheet is public. Inside each of the two
 * zones, a line starting with "##" opens a story tag (see StoryTag) up to the next "##" line or the
 * end of the zone : the text before a zone's first "##" is that zone's permanent text, always in force.
 * The tag is the first word of the "##" line, the rest is a comment ; the line itself is never part
 * of the text. A single "# xxx" line is plain text, so sheets written before tags exist read the same.
 */
public final class NpcSheetParser {

    public static final String SECRET_MARKER_LINE = "# SECRET";
    private static final Pattern TAG_LINE = Pattern.compile("^##(?!#)[ \\t]*(\\S*)(.*)$");

    private NpcSheetParser() {}

    /** name is the "# Name" of the first line, already read by the caller ("" when none). */
    public static Npc parse(String id, String name, String content) {
        String[] lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        int secretLine = lines.length;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].strip().equalsIgnoreCase(SECRET_MARKER_LINE)) { secretLine = i; break; }
        }
        List<StoryTag> tags = new ArrayList<>();
        String publicInfo = zone(id, lines, 0, secretLine, CharacterDelta.Visibility.PUBLIC, tags);
        String secretInfo = secretLine >= lines.length ? ""
            : zone(id, lines, secretLine + 1, lines.length, CharacterDelta.Visibility.SECRET, tags);
        return new Npc(id, name, publicInfo, secretInfo, tags);
    }

    /** The permanent text of lines [from, to) ; the tags found in it are added to `tags`. */
    private static String zone(String id, String[] lines, int from, int to, CharacterDelta.Visibility visibility,
                               List<StoryTag> tags) {
        StringBuilder permanent = new StringBuilder();
        String tagName = null;
        StringBuilder tagText = new StringBuilder();
        for (int i = from; i < to; i++) {
            Matcher header = TAG_LINE.matcher(lines[i].strip());
            if (header.matches()) {
                closeTag(id, tagName, tagText, visibility, tags);
                tagName = header.group(1);
                tagText = new StringBuilder();
                if (tagName.isEmpty()) {
                    throw new IllegalArgumentException("fiche " + id + " ligne " + (i + 1) + " : un titre ## doit commencer par le nom du tag (## nom commentaire).");
                }
                if (tagName.equalsIgnoreCase("SECRET")) {
                    throw new IllegalArgumentException("fiche " + id + " ligne " + (i + 1) + " : SECRET est reserve a la ligne \"# SECRET\", pas a un tag.");
                }
            } else if (tagName == null) {
                permanent.append(lines[i]).append('\n');
            } else {
                tagText.append(lines[i]).append('\n');
            }
        }
        closeTag(id, tagName, tagText, visibility, tags);
        return permanent.toString().strip();
    }

    private static void closeTag(String id, String tagName, StringBuilder text, CharacterDelta.Visibility visibility,
                                 List<StoryTag> tags) {
        if (tagName == null) return;
        if (text.toString().isBlank()) {
            throw new IllegalArgumentException("fiche " + id + " : le tag ## " + tagName + " est vide.");
        }
        if (tags.stream().anyMatch(t -> t.isNamed(tagName))) {
            throw new IllegalArgumentException("fiche " + id + " : le tag ## " + tagName + " existe deja (un nom de tag est unique dans une fiche).");
        }
        tags.add(new StoryTag(tagName, visibility, text.toString().strip()));
    }
}
