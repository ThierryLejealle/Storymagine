package storymagine.chat.coeur.domaine.scenario;

/**
 * A change an author scripts for one character from the act where it is written : PUBLIC adds a line
 * to the character's public sheet (every character present or teammate knows it : a wound, a visible
 * change), SECRET adds it to the secret sheet (only that character reads it : a new goal, a
 * discovery kept private). npcName is how the author named the character (see Cast.findByName).
 */
public record CharacterDelta(Visibility visibility, String npcName, String text) {

    public enum Visibility { PUBLIC, SECRET }
}
