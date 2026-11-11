package storymagine.chat.coeur.domaine.scenario;

/**
 * Who speaks first when a story or an act begins : nobody in particular (NONE — the player opens
 * as usual), any present Npc picked at random (ANY), or one Npc named by the author (named). Set by
 * the author with "{npc-starts}" / "{npc-starts Name}" (the Npc opens in normal mode : it simply
 * speaks, no player message involved, see ChatServiceImpl.openScene) or with "{npc-do}" /
 * "{npc-do Name}" (the Npc opens holding the narrator's pen : it answers as if the player had clicked
 * DO with nothing to say, see ChatServiceImpl.sendMessage). See NpcOpeningMarker.
 * namesText is only meaningful for a named opening, null otherwise : the names as the author wrote them,
 * one Npc or several ("Elena, Marc") — the first one opens, the others follow in the same round, in the
 * order written (the names are read by ActivityResolver.openingNpcs). doPen is true for "{npc-do}".
 */
public record NpcOpening(boolean npcStarts, String namesText, boolean doPen) {

    public static final NpcOpening NONE   = new NpcOpening(false, null, false);
    public static final NpcOpening ANY    = new NpcOpening(true, null, false);
    public static final NpcOpening ANY_DO = new NpcOpening(true, null, true);

    /** "{npc-starts Name, Name}" : a normal opening ; blank names mean any present Npc. */
    public static NpcOpening named(String namesText) {
        return namesText == null || namesText.isBlank() ? ANY : new NpcOpening(true, namesText.strip(), false);
    }

    /** "{npc-do Name, Name}" : an opening with the narrator's pen ; blank names mean any present Npc. */
    public static NpcOpening namedDo(String namesText) {
        return namesText == null || namesText.isBlank() ? ANY_DO : new NpcOpening(true, namesText.strip(), true);
    }

    public boolean isNone()  { return !npcStarts; }
    public boolean isNamed() { return npcStarts && namesText != null; }
}
