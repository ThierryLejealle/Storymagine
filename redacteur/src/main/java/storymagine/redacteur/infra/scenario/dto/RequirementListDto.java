package storymagine.redacteur.infra.scenario.dto;

import java.util.List;

/**
 * DTO for the requirements field at chapter/sequence level.
 * Supports two YAML forms:
 *
 * Flat list (backward compat) — items go to both plan and writer:
 *   requirements:
 *     - "constraint | check"
 *
 * Structured (explicit split):
 *   requirements:
 *     plan:
 *       - "item for plan critic"
 *     writer:
 *       - "item for writer critic"
 */
public class RequirementListDto {

    /** Items that apply to both plan and writer (from flat list form). */
    public List<String> global;

    /** Items for the plan phase only. */
    public List<String> plan;

    /** Items for the writer phase only. */
    public List<String> writer;
}
