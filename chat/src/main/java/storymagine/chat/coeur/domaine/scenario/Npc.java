package storymagine.chat.coeur.domaine.scenario;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One character playable by the LLM in a Cast : id is the storage adapter's identifier (the
 * source file's name, without extension — see Cast), name is the display name (empty if the
 * source file declares none, same "# Name" heading convention formerly on ChatScenario.
 * characterName). publicInfo is what any other character/the player can be told about this Npc
 * (traits observable through interaction) ; secretInfo is known only to the Npc itself — never
 * shown to another Npc's prompt, only included when this Npc is the one speaking. Both verbatim
 * from the source file (headings kept in place, not stripped). secretInfo is "" when the file has
 * no "# SECRET" section (see NpcSheetParser) — a purely public character. Both are the PERMANENT
 * text ; tags are the "## tag" sections (see StoryTag), added to the sheet only while switched on
 * (see withActiveTags).
 */
public record Npc(String id, String name, String publicInfo, String secretInfo, List<StoryTag> tags) {

    public Npc {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public Npc(String id, String name, String publicInfo, String secretInfo) {
        this(id, name, publicInfo, secretInfo, List.of());
    }

    public boolean hasTag(String tag) {
        return tags.stream().anyMatch(t -> t.isNamed(tag));
    }

    /**
     * The same Npc with the text of every tag named in `active` (lowercase names) added to its public
     * or secret sheet, in the order of the file — whatever the order they were switched on.
     */
    public Npc withActiveTags(Set<String> active) {
        Npc result = this;
        for (StoryTag tag : tags) {
            if (!active.contains(tag.name().toLowerCase(Locale.ROOT))) continue;
            result = tag.visibility() == CharacterDelta.Visibility.PUBLIC
                ? result.withPublicBlock(tag.text()) : result.withSecretBlock(tag.text());
        }
        return result;
    }

    private Npc withPublicBlock(String text) {
        return new Npc(id, name, publicInfo.isBlank() ? text : publicInfo + "\n\n" + text, secretInfo, tags);
    }

    private Npc withSecretBlock(String text) {
        return new Npc(id, name, publicInfo, secretInfo.isBlank() ? text : secretInfo + "\n\n" + text, tags);
    }

    /** Everything this Npc knows about themselves — used to build their own prompt when they speak. */
    public String fullSheet() {
        return secretInfo.isBlank() ? publicInfo : publicInfo + "\n\n" + secretInfo;
    }

    /** The same Npc with one more line on its public sheet (see CharacterDelta). */
    public Npc withPublicAddition(String line) {
        return new Npc(id, name, publicInfo.isBlank() ? line : publicInfo + "\n" + line, secretInfo, tags);
    }

    /** The same Npc with one more line on its secret sheet (see CharacterDelta). */
    public Npc withSecretAddition(String line) {
        return new Npc(id, name, publicInfo, secretInfo.isBlank() ? line : secretInfo + "\n" + line, tags);
    }

    /** Display label : name if declared, the storage id otherwise (always unique, unlike a possibly-empty name). */
    public String label() {
        return name == null || name.isBlank() ? id : name;
    }
}
