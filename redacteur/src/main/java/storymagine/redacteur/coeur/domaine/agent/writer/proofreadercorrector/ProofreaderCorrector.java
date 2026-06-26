package storymagine.redacteur.coeur.domaine.agent.writer.proofreadercorrector;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects language errors in a sequence and returns (wrong phrase, corrected phrase) pairs.
 * Corrections are applied in Java by the service layer — no second LLM call.
 * Source: ProofreaderContext.analyze.
 */
public class ProofreaderCorrector implements Agent {

    private static final String SYSTEM = """
        Tu analyses ce texte et identifies toutes les fautes de langue :
        - fautes de grammaire, d'orthographe, d'accord et de conjugaison
        - mots manquants, absents ou mal choisis (emploi sémantiquement inapproprié)
        - phrases bancales : calques d'une autre langue, formulations maladroites,
          pléonasmes, syntaxe confuse ou non idiomatique dans la langue du texte
        - ou tout autre problème qui sonne faux dans la langue du texte
        Pour chaque problème, cite la phrase complète telle qu'elle apparaît dans le texte,
        puis donne la même phrase corrigée.
        Format STRICT —
        Pour chaque problème :
        CORRECTIONS:
        - FAUX: "phrase exacte contenant le problème"
          JUSTE: "phrase corrigée"

        Exemple :
        CORRECTIONS:
        - FAUX: "Il a été allé au marché hier soir."
          JUSTE: "Il est allé au marché hier soir."

        ATTENTION : une seule phrase par ligne JUSTE, sans variante, sans commentaire, sans explication.

        Si aucun problème : retourner "PAS D'ERREUR" — rien avant, rien après.""";

    private static final String AGENT_NAME = "ProofreaderCorrector";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ProofreaderCorrector(ModelCallPort llm) {
        this.llm = llm;
    }

    public ProofreaderCorrectorOutput call(ProofreaderCorrectorInput input) {
        int maxChars = llm.contextWindow() * 4 / 3;
        String user  = "### Texte\n" + trunc(input.text(), maxChars) + "\n\nAnalyse les fautes.";
        String raw   = llm.generate(SYSTEM, user, 0.1, LlmCallContext.of(agentName(), agentLabel())).text();
        return new ProofreaderCorrectorOutput(parseCorrections(raw));
    }

    private List<ProofreaderCorrectorOutput.Correction> parseCorrections(String response) {
        if (response == null || response.trim().startsWith("PAS D'ERREUR")) return List.of();
        List<ProofreaderCorrectorOutput.Correction> result = new ArrayList<>();
        String[] lines = response.split("\n");
        String wrong = null;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("- FAUX:") || t.startsWith("FAUX:")) {
                wrong = unquote(t.replaceFirst("^-?\\s*FAUX:", "").trim());
            } else if (t.startsWith("JUSTE:") && wrong != null) {
                String correct = unquote(t.substring("JUSTE:".length()).trim());
                if (!wrong.isBlank() && !correct.isBlank())
                    result.add(new ProofreaderCorrectorOutput.Correction(wrong, correct));
                wrong = null;
            }
        }
        return result;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1);
        return s;
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
