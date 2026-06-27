package storymagine.redacteur.coeur.domaine.agent.commun.planenricher;

/** Maps a directive field to the JSON key name used in the enriched plan. */
public record FieldMapping(DirectiveField source, String jsonLabel) {}
