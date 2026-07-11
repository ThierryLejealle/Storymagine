package storymagine.redacteur.coeur.domaine.scenario;

import java.util.List;

/** Global requirements from requirements.md (or chapter/sequence YAML), split by audience. */
public class RequirementList {

    public static final RequirementList EMPTY = new RequirementList(List.of(), List.of());

    private final List<Requirement> planRequirements;
    private final List<Requirement> writerRequirements;

    public RequirementList(List<Requirement> planRequirements, List<Requirement> writerRequirements) {
        this.planRequirements   = List.copyOf(planRequirements);
        this.writerRequirements = List.copyOf(writerRequirements);
    }

    public List<Requirement> planRequirements()   { return planRequirements; }
    public List<Requirement> writerRequirements() { return writerRequirements; }
}
