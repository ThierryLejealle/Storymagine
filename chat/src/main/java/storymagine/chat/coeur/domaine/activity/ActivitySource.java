package storymagine.chat.coeur.domaine.activity;

/**
 * Where a character's activity level comes from : IMPLICIT (the default, or a command without any
 * name), EXPLICIT (a scenario command that names the character) or FORCED (the player clicked it).
 * Only an IMPLICIT level can be changed by a later command without a name.
 */
public enum ActivitySource {
    IMPLICIT, EXPLICIT, FORCED
}
