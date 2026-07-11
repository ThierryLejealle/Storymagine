package storymagine.redacteur.coeur.domaine.agent.temp.focusactionfilter;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Selects which focus groups and action categories are relevant for the current sequence.
 * Returns only names, not items. Falls back to the full list if the LLM omits a line.
 * Source: LlmFilterContext.filter.
 */
public class FocusActionFilter implements Agent {

    private static final String SYSTEM = """
        Tu filtres les ressources narratives pour une séquence à écrire.
        On te donne des groupes de focus (précédés de [NOM]) et des catégories d'action.
        Sélectionne uniquement ceux qui correspondent à ce que la séquence décrite devrait contenir.

        Pour FOCUS : retourne les noms tels qu'ils apparaissent entre crochets (ex: DESTRUCTION_ENNEMI).
        Jamais les items individuels — uniquement le nom du groupe.
        Pour ACTIONS : retourne les noms de catégories tels qu'ils sont listés.

        Format exact — deux lignes, rien d'autre :
        FOCUS: NOM_GROUPE1, NOM_GROUPE2
        ACTIONS: cat1, cat2
        Si rien n'est pertinent pour une ligne, laisse la liste vide après les deux-points.""";

    private static final String AGENT_NAME = "FocusActionFilter";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public FocusActionFilter(ModelCallPort llm) {
        this.llm = llm;
    }

    public FocusActionFilterOutput call(FocusActionFilterInput input) {
        String sequenceSection   = "### Séquence à écrire\n" + input.sequenceDescription() + "\n\n";

        String conditionSection  = (input.condition() != null && !input.condition().isBlank())
                ? "### Condition narrative\n" + input.condition() + "\n\n" : "";

        String repetitionSection = (input.avoidRepetition() && !input.alreadyUsed().isEmpty())
                ? "### Déjà utilisé dans ce livre (évite la répétition)\n"
                  + String.join(", ", input.alreadyUsed()) + "\n\n" : "";

        String focusSection      = input.groupsText().isBlank() ? ""
                : "### Groupes de focus disponibles\n" + input.groupsText() + "\n\n";

        String actionsSection    = input.actionsText().isBlank() ? ""
                : "### Catégories d'action disponibles\n" + String.join(", ", input.actionCategories()) + "\n\n";

        String user = sequenceSection
                + conditionSection
                + repetitionSection
                + focusSection
                + actionsSection
                + "Filtre et retiens uniquement les éléments pertinents.";

        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return parseResponse(raw, input.groupNames(), input.actionCategories());
    }

    private FocusActionFilterOutput parseResponse(String response,
                                                   List<String> validGroups,
                                                   List<String> validActions) {
        List<String> groups  = null;
        List<String> actions = null;
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (t.toUpperCase().startsWith("FOCUS:")) {
                groups  = parseList(t.substring(6), validGroups);
            } else if (t.toUpperCase().startsWith("ACTIONS:")) {
                actions = parseList(t.substring(8), validActions);
            }
        }
        // If LLM omitted the line entirely, fall back to full list
        if (groups  == null && !validGroups.isEmpty())  groups  = validGroups;
        if (actions == null && !validActions.isEmpty()) actions = validActions;
        return new FocusActionFilterOutput(
            groups  != null ? groups  : List.of(),
            actions != null ? actions : List.of()
        );
    }

    private List<String> parseList(String csv, List<String> valid) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        List<String> validNorm = valid.stream().map(String::toLowerCase).collect(Collectors.toList());
        return Arrays.stream(csv.split(","))
            .map(String::trim).filter(s -> !s.isBlank())
            .filter(s -> validNorm.contains(s.toLowerCase()))
            .map(s -> valid.get(validNorm.indexOf(s.toLowerCase())))
            .collect(Collectors.toList());
    }
}
