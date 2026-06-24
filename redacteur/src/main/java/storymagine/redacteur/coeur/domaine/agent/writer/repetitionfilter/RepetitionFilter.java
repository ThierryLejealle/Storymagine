package storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters out candidate phrases that are semantically covered by a protected leitmotiv.
 * When in doubt, protects the expression (prefers false negatives over false positives).
 * Source: RepetitionFilterContext.filter.
 */
public class RepetitionFilter implements Agent {

    private static final String SYSTEM = """
        Tu es un éditeur littéraire. On te donne :
        1. Une liste de leitmotivs/rituels narratifs intentionnellement récurrents (à protéger).
        2. Une liste d'expressions extraites d'un texte, candidates à être bannies.
        Ta tâche : retourner uniquement les expressions candidates qui NE sont PAS
        sémantiquement proches d'un leitmotiv protégé.
        En cas de doute, protège l'expression (ne la mets pas dans la liste retournée).
        On préfère autoriser quelques répétitions plutôt qu'interdire un leitmotiv par erreur.
        Format de sortie STRICT : une expression par ligne, précédée d'un tiret (-).
        Pas de commentaires. Pas d'explication. Uniquement la liste filtrée. En français.""";

    private static final String AGENT_NAME = "RepetitionFilter";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public RepetitionFilter(ModelCallPort llm) {
        this.llm = llm;
    }

    public RepetitionFilterOutput call(RepetitionFilterInput input) {
        if (input.candidates().isEmpty()
                || input.keepPhrasesContent() == null
                || input.keepPhrasesContent().isBlank()) {
            return new RepetitionFilterOutput(input.candidates());
        }
        String candidateList = input.candidates().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        String user = "### Leitmotivs et rituels protégés (ne pas bannir ces expressions) :\n"
            + input.keepPhrasesContent()
            + "\n\n### Expressions candidates à bannir :\n"
            + candidateList
            + "\n\nRetourne uniquement les expressions à bannir (hors leitmotivs protégés).";
        String raw = llm.generate(SYSTEM, user, 0.2, LlmCallContext.of(agentName())).text();
        return new RepetitionFilterOutput(parseList(raw));
    }

    private static List<String> parseList(String response) {
        List<String> result = new ArrayList<>();
        for (String line : response.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("-") && !t.startsWith("–") && !t.startsWith("•") && !t.startsWith("*")) continue;
            String cleaned = t.replaceAll("^[-–*•\\s]+", "").trim();
            if (!cleaned.isBlank() && cleaned.length() > 5 && cleaned.length() < 120)
                result.add(cleaned);
        }
        return result;
    }
}
