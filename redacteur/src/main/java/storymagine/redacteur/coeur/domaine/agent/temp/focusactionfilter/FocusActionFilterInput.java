package storymagine.redacteur.coeur.domaine.agent.temp.focusactionfilter;

import java.util.List;

/** Input for FocusActionFilter — selects relevant focus groups and action categories per sequence. */
public record FocusActionFilterInput(
    String sequenceDescription,
    List<String> groupNames,
    String groupsText,
    List<String> actionCategories,
    String actionsText,
    String condition,
    boolean avoidRepetition,
    List<String> alreadyUsed
) {}
