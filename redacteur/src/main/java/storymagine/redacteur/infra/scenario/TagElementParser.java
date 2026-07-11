package storymagine.redacteur.infra.scenario;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Markdown files that follow the [TAG] / #PLAN / #WRITER convention
 * used by focus.md, lore.md and character sheets.
 * Optional #COMMON section: resets to GLOBAL (same as content before #PLAN).
 */
class TagElementParser {

    enum Section { GLOBAL, PLAN, WRITER, CHECK }

    /**
     * checkContent is optional and only meaningful for FocusElement — LoreElement and
     * Personnage simply ignore it. Kept on one shared TagBlock so the parsing loop
     * (sections, [TAG] markers, HTML comment stripping) stays mutualized for all callers.
     */
    record TagBlock(String tag, String globalContent, String planContent, String writerContent, String checkContent) {}

    /**
     * Parses a single-block file (no [TAG] markers) into one TagBlock with a null tag.
     * Used for files like character sheets that have GLOBAL / #PLAN / #WRITER sections
     * but no per-tag granularity.
     */
    static TagBlock parseSingleBlock(String content) {
        StringBuilder global = new StringBuilder();
        StringBuilder plan   = new StringBuilder();
        StringBuilder writer = new StringBuilder();
        StringBuilder check  = new StringBuilder();
        Section section = Section.GLOBAL;

        for (String raw : content.split("\n")) {
            String line = raw.stripTrailing();
            if (isHtmlComment(line)) continue;
            if (line.equals("#COMMON") || line.equals("# COMMON")) { section = Section.GLOBAL; continue; }
            if (line.equals("#PLAN")   || line.equals("# PLAN"))   { section = Section.PLAN;   continue; }
            if (line.equals("#WRITER") || line.equals("# WRITER")) { section = Section.WRITER; continue; }
            if (line.equals("#CHECK")  || line.equals("# CHECK"))  { section = Section.CHECK;  continue; }
            appendTo(section, global, plan, writer, check, line);
        }

        return new TagBlock(null,
                trimOrNull(global.toString()),
                trimOrNull(plan.toString()),
                trimOrNull(writer.toString()),
                trimOrNull(check.toString()));
    }

    /** Parses a full file content into a list of TagBlocks. */
    static List<TagBlock> parse(String content) {
        List<TagBlock> result   = new ArrayList<>();
        Section        section  = Section.GLOBAL;

        String  currentTag    = null;
        StringBuilder global  = new StringBuilder();
        StringBuilder plan    = new StringBuilder();
        StringBuilder writer  = new StringBuilder();
        StringBuilder check   = new StringBuilder();

        for (String raw : content.split("\n")) {
            String line = raw.stripTrailing();

            if (isHtmlComment(line)) continue;

            if (line.equals("#COMMON") || line.equals("# COMMON")) { section = Section.GLOBAL; continue; }
            if (line.equals("#PLAN")   || line.equals("# PLAN"))   { section = Section.PLAN;   continue; }
            if (line.equals("#WRITER") || line.equals("# WRITER")) { section = Section.WRITER; continue; }
            if (line.equals("#CHECK")  || line.equals("# CHECK"))  { section = Section.CHECK;  continue; }

            if (line.startsWith("[") && line.contains("]")) {
                int close = line.indexOf(']');
                String newTag = line.substring(1, close).trim();
                if (!newTag.isBlank()) {
                    if (currentTag != null) {
                        result.add(flush(currentTag, global, plan, writer, check));
                        global.setLength(0); plan.setLength(0); writer.setLength(0); check.setLength(0);
                    }
                    section    = Section.GLOBAL;
                    currentTag = newTag;
                    String rest = line.substring(close + 1).trim();
                    if (!rest.isBlank()) appendTo(section, global, plan, writer, check, rest);
                    continue;
                }
            }

            if (currentTag != null) {
                appendTo(section, global, plan, writer, check, line);
            }
        }

        if (currentTag != null) {
            result.add(flush(currentTag, global, plan, writer, check));
        }

        return result;
    }

    private static void appendTo(Section s, StringBuilder g, StringBuilder p, StringBuilder w, StringBuilder c, String line) {
        switch (s) {
            case GLOBAL -> g.append(line).append('\n');
            case PLAN   -> p.append(line).append('\n');
            case WRITER -> w.append(line).append('\n');
            case CHECK  -> c.append(line).append('\n');
        }
    }

    private static TagBlock flush(String tag, StringBuilder g, StringBuilder p, StringBuilder w, StringBuilder c) {
        return new TagBlock(tag,
                trimOrNull(g.toString()),
                trimOrNull(p.toString()),
                trimOrNull(w.toString()),
                trimOrNull(c.toString()));
    }

    static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }

    private static boolean isHtmlComment(String line) {
        return line.startsWith("<!--") || line.startsWith("-->") || line.startsWith("  ");
    }
}
