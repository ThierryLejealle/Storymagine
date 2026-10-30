package storymagine.chat.coeur.domaine.validation;

/** One text file of a scenario as the author wrote it (scenario.txt, scenario-1.txt...), before any pick is drawn. */
public record ScenarioSource(String fileName, String text) {}
