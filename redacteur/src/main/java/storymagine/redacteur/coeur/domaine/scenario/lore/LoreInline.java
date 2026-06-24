package storymagine.redacteur.coeur.domaine.scenario.lore;

/** Inline lore text written directly in the chapter — not from the pool. */
public record LoreInline(String text) implements LoreItem {}
