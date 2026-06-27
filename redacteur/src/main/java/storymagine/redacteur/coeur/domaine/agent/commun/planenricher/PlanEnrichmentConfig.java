package storymagine.redacteur.coeur.domaine.agent.commun.planenricher;

import java.util.List;

/** Declares which directive fields to inject into a plan JSON and under what JSON labels. */
public record PlanEnrichmentConfig(List<FieldMapping> mappings) {

    public static PlanEnrichmentConfig of(FieldMapping... mappings) {
        return new PlanEnrichmentConfig(List.of(mappings));
    }
}
