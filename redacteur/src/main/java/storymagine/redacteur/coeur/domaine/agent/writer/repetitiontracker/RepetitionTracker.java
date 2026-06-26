package storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts distinctive phrases and narrative patterns to avoid in future sequences.
 * Maintains two sliding windows: lexical expressions (EXPRESSIONS) and abstract patterns (SCHÉMAS).
 * Source: RepetitionTrackerContext.extract.
 */
public class RepetitionTracker implements Agent {

    private static final String SYSTEM = """
        Tu es un éditeur littéraire qui détecte les répétitions dans un roman.
        Ta tâche : extraire du texte deux listes d'éléments à éviter dans les séquences suivantes.

        EXPRESSIONS — tournures lexicales distinctives (3 mots minimum) : métaphores, images
        atmosphériques, formulations caractéristiques. Ces expressions ne doivent pas réapparaître
        telles quelles ou quasi-telles dans les séquences suivantes.
        Entre 3 et 8 expressions.

        SCHÉMAS — patterns narratifs récurrents décrits de façon abstraite : comportement
        récurrent d'un personnage, ambiance systématiquement revisitée, structure de scène
        répétitive, sensation physique toujours décrite de la même manière.
        Décris le pattern en une courte phrase neutre — pas la formulation exacte, le concept.
        Entre 2 et 5 schémas.

        Format de sortie STRICT — deux sections, rien d'autre :
        EXPRESSIONS:
        - expression 1
        - expression 2

        SCHÉMAS:
        - schéma 1
        - schéma 2

        Pas de commentaires. Pas d'explication. En français.""";

    private static final String AGENT_NAME = "RepetitionTracker";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public RepetitionTracker(ModelCallPort llm) {
        this.llm = llm;
    }

    public RepetitionTrackerOutput call(RepetitionTrackerInput input) {
        if (input.text() == null || input.text().isBlank())
            return new RepetitionTrackerOutput(List.of(), List.of());

        String phrasesList = input.alreadyPhrases().isEmpty()
            ? "Aucune pour l'instant."
            : input.alreadyPhrases().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        String themesList = input.alreadyThemes().isEmpty()
            ? "Aucun pour l'instant."
            : input.alreadyThemes().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));

        String user = "### Expressions déjà traquées (ne pas répéter dans EXPRESSIONS) :\n"
            + phrasesList
            + "\n\n### Schémas déjà traqués (ne pas répéter dans SCHÉMAS) :\n"
            + themesList
            + "\n\n### Texte à analyser :\n"
            + trunc(input.text(), 10000)
            + "\n\nExtrais les expressions et schémas de ce texte.";

        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        return parseResult(raw);
    }

    private static RepetitionTrackerOutput parseResult(String response) {
        List<String> phrases = new ArrayList<>();
        List<String> themes  = new ArrayList<>();
        List<String> current = null;
        for (String line : response.split("\n")) {
            String t = line.trim();
            String upper = t.toUpperCase();
            if (upper.startsWith("EXPRESSIONS")) {
                current = phrases;
            } else if (upper.startsWith("SCH")) {
                current = themes;
            } else if (current != null && (t.startsWith("-") || t.startsWith("–") || t.startsWith("•"))) {
                String cleaned = t.replaceAll("^[-–•*\\s]+", "").trim();
                if (!cleaned.isBlank() && cleaned.length() > 5 && cleaned.length() < 120)
                    current.add(cleaned);
            }
        }
        return new RepetitionTrackerOutput(phrases, themes);
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
