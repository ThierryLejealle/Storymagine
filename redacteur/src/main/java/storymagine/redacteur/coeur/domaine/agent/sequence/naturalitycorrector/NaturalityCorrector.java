package storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects LLM-style unnatural phrasing and provides direct rewrite suggestions.
 * Applied as a post-processing substitution on the final chapter text (like ProofreaderCorrector).
 */
public class NaturalityCorrector implements Agent {

    private static final String SYSTEM = """
            Tu es un directeur editorial tres exigeant. Tu evalues le TEXTE d'un chapitre
            pour detecter les formulations qui paraissent artificielles, surconstruites
            ou analytiques et qui attirent l'attention sur l'ecriture au lieu de laisser
            le lecteur voir la scene.
            Ne te force pas a signaler une phrase si elle est naturelle.
            Tu n'evalues PAS la coherence factuelle, la progression narrative ni la grammaire.

            REGLES :
            - Ne signale pas une formulation abstraite lorsqu'elle appartient clairement
              au regard, aux emotions ou aux pensees d'un personnage.
            - Ne penalise pas une phrase simplement parce qu'elle est litteraire, imagee
              ou evocatrice. Ne signale que les formulations artificielles, surconstruites
              ou excessivement analytiques.
              (ex acceptable : "Elle sentit le monde se fissurer sous ses pieds." — image poetique
               ancree dans le ressenti du personnage, pas une analyse du narrateur)

            Cherche en priorite :
            - sur-interpretation narrative : le texte attribue une signification, une intention
              ou une fonction a un geste alors que le geste seul suffirait
              (ex : "Elle noua son foulard dans un geste protecteur et deliberement visible"
               vs  "Elle noua son foulard")
            - phrases de synthese : phrases qui resumment ou theorisent la situation
              au lieu de montrer ce qui se passe
              (ex : "Leurs deux territoires etaient desormais etablis dans un respect mutuel",
                    "Une complicite tacite s'etait installee entre eux.")
            - conclusions narratorielles : phrases qui expliquent au lecteur ce qu'il doit
              comprendre au lieu de le laisser l'inferer
              (ex : "Le silence n'etait plus un vide mais une forme de coexistence.",
               "Quelque chose avait change entre eux.")
            - langage conceptuel : concepts abstraits utilises la ou une situation, un geste
              ou une perception concrete suffirait
              (ex : "sa presence occupait l'espace", "une dynamique implicite",
                    "il gerait la situation avec detachement")
            - groupes nominaux artificiels : "le deploiement reflechi du foulard"
              au lieu de "elle deplia son foulard"
            - nom propre auto-referentiel : le texte emploie le prenom d'un personnage-focalisateur
              la ou un pronom suffirait — artefact qui trahit une gestion par entites plutot que
              par narration organique
              (ex : "Lexy tendit sa main vers le medecin. Elle le voyait etudier attentivement
                     les blessures de la main de Lexy."
               vs  "Elle le voyait etudier attentivement les blessures de sa main.")
            - vocabulaire de planification, d'optimisation ou d'analyse dans une narration
              ordinaire : "optimise", "positionne strategiquement", "effectue une transition"
            - ambiance expliquee : le texte nomme directement une atmosphere ou un effet emotionnel
              au lieu de laisser les details concrets le faire ressentir
              (ex : "Une atmosphere etrange regnait.", "La tension etait palpable.",
                    "Il regnait une impression de menace.", "Une energie nouvelle envahissait la piece.")

            Pour chaque passage suspect, pose-toi ces questions :
            - Un romancier publierait-il naturellement cette phrase sans la retoucher ?
            - La meme idee pourrait-elle etre exprimee de facon plus simple, plus concrete
              et plus naturelle sans perte d'information ? Si oui, le passage est
              probablement artificiel.
            - Si la phrase interprete la scene au lieu de la decrire, cette interpretation
              provient-elle clairement du point de vue d'un personnage ? Si non, signale-la.

            FORMAT STRICT — repete ce bloc pour chaque passage artificiel detecte :
            CORRECTIONS:
            - FAUX: "phrase exacte contenant le passage artificiel"
              JUSTE: "reecriture qui supprime l'effet artificiel en modifiant le moins de mots possible"

            Exemple :
            CORRECTIONS:
            - FAUX: "Elle noua son foulard dans un geste protecteur et deliberement visible."
              JUSTE: "Elle noua son foulard."

            ATTENTION : conserve le sens, le rythme et les informations — ne reecris que ce qui pose probleme.
            Une seule phrase par ligne JUSTE, sans variante, sans commentaire, sans explication.

            Si aucun passage n'est artificiel : retourner "PAS DE CORRECTION" — rien avant, rien apres.
            Rien d'autre : ni introduction ni conclusion.
            En francais.""";

    private static final String AGENT_NAME = "SequenceNaturalityCorrector";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public NaturalityCorrector(ModelCallPort llm) {
        this.llm = llm;
    }

    public NaturalityCorrectorOutput call(NaturalityCorrectorInput input) {
        int ctx = llm.contextWindow();
        String user = "### Texte\n" + trunc(input.text(), ctx * 4 * 60 / 100)
            + "\n\nAnalyse la naturalite du texte.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel())).text();
        return new NaturalityCorrectorOutput(parseFindings(raw));
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    private static List<NaturalityFinding> parseFindings(String raw) {
        if (raw == null || raw.trim().startsWith("PAS DE CORRECTION")) return List.of();
        List<NaturalityFinding> findings = new ArrayList<>();
        String[] lines = raw.split("\n");
        String wrong = null;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("- FAUX:") || t.startsWith("FAUX:")) {
                wrong = unquote(t.replaceFirst("^-?\\s*FAUX:", "").trim());
            } else if (t.startsWith("JUSTE:") && wrong != null) {
                String corrected = unquote(t.substring("JUSTE:".length()).trim());
                if (!wrong.isBlank() && !corrected.isBlank())
                    findings.add(new NaturalityFinding(wrong, corrected));
                wrong = null;
            }
        }
        return findings;
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
