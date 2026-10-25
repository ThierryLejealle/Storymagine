package storymagine.chat.coeur.domaine.scenario;

/**
 * Who speaks first when a story or an act begins : nobody in particular (NONE — the player opens
 * as usual), any present Npc picked at random (ANY), or one Npc named by the author (named). Set by
 * the author with "[NPC STARTS]" / "[NPC STARTS: Name]" (see NpcOpeningMarker) ; the Npc then
 * answers as if the player had clicked DO with nothing to say (see ChatServiceImpl.sendMessage).
 * npcName is only meaningful for a named opening, null otherwise.
 */
public record NpcOpening(boolean npcStarts, String npcName) {

    public static final NpcOpening NONE = new NpcOpening(false, null);
    public static final NpcOpening ANY  = new NpcOpening(true, null);

    public static NpcOpening named(String npcName) {
        return npcName == null || npcName.isBlank() ? ANY : new NpcOpening(true, npcName.strip());
    }

    public boolean isNone()  { return !npcStarts; }
    public boolean isNamed() { return npcStarts && npcName != null; }
}
