package storymagine.redacteur.coeur.domaine.agent.temp.focusactionfilter;

import java.util.List;

/** Output of FocusActionFilter — names of selected focus groups and action categories. */
public record FocusActionFilterOutput(List<String> focusGroups, List<String> actionCategories) {}
