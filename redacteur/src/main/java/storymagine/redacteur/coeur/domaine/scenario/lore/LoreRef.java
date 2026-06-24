package storymagine.redacteur.coeur.domaine.scenario.lore;

/** Reference to a LoreElement by tag name — resolved against LorePool at load time. */
public record LoreRef(String tag, LoreElement resolved) implements LoreItem {}
