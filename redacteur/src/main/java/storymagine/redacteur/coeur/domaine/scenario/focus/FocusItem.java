package storymagine.redacteur.coeur.domaine.scenario.focus;

/** A focus directive inside a chapter or sequence: either a pool reference or inline text. */
public sealed interface FocusItem permits FocusRef, FocusInline {}
