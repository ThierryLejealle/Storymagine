package storymagine.chat.coeur.domaine.scenario;

/**
 * Who speaks first when a story or an act begins : nobody in particular (NONE — the player opens
 * as usual), any present Npc picked at random (ANY), or one Npc named by the author (named). Set by
 * the author with "{npc-starts}" / "{npc-starts Name}" (the Npc opens in normal mode : it simply
 * speaks, no player message involved, see ChatServiceImpl.openScene) or with "{npc-do}" /
 * "{npc-do Name}" (the Npc opens holding the narrator's pen : it answers as if the player had clicked
 * DO with nothing to say, see ChatServiceImpl.sendMessage). See NpcOpeningMarker.
 * npcName is only meaningful for a named opening, null otherwise. doPen is true for "{npc-do}".
 */
public record NpcOpening(boolean npcStarts, String npcName, boolean doPen) {

    public static final NpcOpening NONE   = new NpcOpening(false, null, false);
    public static final NpcOpening ANY    = new NpcOpening(true, null, false);
    public static final NpcOpening ANY_DO = new NpcOpening(true, null, true);

    /** "{npc-starts Name}" : a normal opening ; a blank name means any present Npc. */
    public static NpcOpening named(String npcName) {
        return npcName == null || npcName.isBlank() ? ANY : new NpcOpening(true, npcName.strip(), false);
    }

    /** "{npc-do Name}" : an opening with the narrator's pen ; a blank name means any present Npc. */
    public static NpcOpening namedDo(String npcName) {
        return npcName == null || npcName.isBlank() ? ANY_DO : new NpcOpening(true, npcName.strip(), true);
    }

    public boolean isNone()  { return !npcStarts; }
    public boolean isNamed() { return npcStarts && npcName != null; }
}
