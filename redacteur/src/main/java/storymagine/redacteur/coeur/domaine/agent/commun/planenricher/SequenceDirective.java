package storymagine.redacteur.coeur.domaine.agent.commun.planenricher;

/** Pre-formatted directive data for one sequence, ready to inject into a plan JSON. */
public record SequenceDirective(
    String checks,
    String focus,
    String lore,
    String constraints
) {
    public static final SequenceDirective EMPTY = new SequenceDirective("", "", "", "");
}
