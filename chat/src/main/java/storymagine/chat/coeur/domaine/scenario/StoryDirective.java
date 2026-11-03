package storymagine.chat.coeur.domaine.scenario;

/**
 * A "{npc-story on|off Name tag}" command : from the act where it is written, and for every act after
 * it, the "## tag" section of that character's sheet is in force (on) or not (off), until a later
 * command says otherwise. npcName is how the author named the character (see Cast.findByName).
 */
public record StoryDirective(boolean on, String npcName, String tag) {
}
