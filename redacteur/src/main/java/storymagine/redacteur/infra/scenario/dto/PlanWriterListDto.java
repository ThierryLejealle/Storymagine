package storymagine.redacteur.infra.scenario.dto;

import java.util.List;

/**
 * Shared DTO for checks and constraints at chapter/sequence level.
 * Supports two YAML forms:
 *
 * Flat list (backward compat) — items go to both plan and writer:
 *   checks:
 *     - "item"
 *
 * Structured (explicit split):
 *   checks:
 *     plan:
 *       - "item for plan critic"
 *     writer:
 *       - "item for writer critic"
 */
public class PlanWriterListDto {

    /** Items that apply to both plan and writer (from flat list form). */
    public List<String> global;

    /** Items for the plan critic only. */
    public List<String> plan;

    /** Items for the writer critic only. */
    public List<String> writer;
}
