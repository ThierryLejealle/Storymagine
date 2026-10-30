package storymagine.chat.coeur.domaine.scenario;

/**
 * One character playable by the LLM in a Cast : id is the storage adapter's identifier (the
 * source file's name, without extension — see Cast), name is the display name (empty if the
 * source file declares none, same "# Name" heading convention formerly on ChatScenario.
 * characterName). publicInfo is what any other character/the player can be told about this Npc
 * (traits observable through interaction) ; secretInfo is known only to the Npc itself — never
 * shown to another Npc's prompt, only included when this Npc is the one speaking. Both verbatim
 * from the source file (headings kept in place, not stripped). secretInfo is "" when the file has
 * no "# SECRET" section (see ChatFileStorageAdapter) — a purely public character.
 */
public record Npc(String id, String name, String publicInfo, String secretInfo) {

    /** Everything this Npc knows about themselves — used to build their own prompt when they speak. */
    public String fullSheet() {
        return secretInfo.isBlank() ? publicInfo : publicInfo + "\n\n" + secretInfo;
    }

    /** The same Npc with one more line on its public sheet (see CharacterDelta). */
    public Npc withPublicAddition(String line) {
        return new Npc(id, name, publicInfo.isBlank() ? line : publicInfo + "\n" + line, secretInfo);
    }

    /** The same Npc with one more line on its secret sheet (see CharacterDelta). */
    public Npc withSecretAddition(String line) {
        return new Npc(id, name, publicInfo, secretInfo.isBlank() ? line : secretInfo + "\n" + line);
    }

    /** Display label : name if declared, the storage id otherwise (always unique, unlike a possibly-empty name). */
    public String label() {
        return name == null || name.isBlank() ? id : name;
    }
}
