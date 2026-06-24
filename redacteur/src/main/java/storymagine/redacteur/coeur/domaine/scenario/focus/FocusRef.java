package storymagine.redacteur.coeur.domaine.scenario.focus;

/** Reference to a FocusElement by tag name — resolved against FocusPool at load time. */
public record FocusRef(String tag, FocusElement resolved) implements FocusItem {}
