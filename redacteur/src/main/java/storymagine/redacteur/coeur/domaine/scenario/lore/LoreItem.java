package storymagine.redacteur.coeur.domaine.scenario.lore;

/** A lore directive inside a chapter or sequence: either a pool reference or inline text. */
public sealed interface LoreItem permits LoreRef, LoreInline {}
