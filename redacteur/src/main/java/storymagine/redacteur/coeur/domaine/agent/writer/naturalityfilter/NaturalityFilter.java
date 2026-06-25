package storymagine.redacteur.coeur.domaine.agent.writer.naturalityfilter;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects LLM-style unnatural phrasing and provides direct rewrite suggestions.
 * Applied as a post-processing substitution on the final chapter text (like Proofreader).
 */
public class NaturalityFilter implements Agent {

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

            Cherche en priorite :
            - sur-interpretation narrative : le texte attribue une signification, une intention
              ou une fonction a un geste alors que le geste seul suffirait
              (ex : "Elle noua son foulard dans un geste protecteur et deliberement visible"
               vs  "Elle noua son foulard")
            - phrases de synthese : phrases qui resumment ou theorisent la situation
              au lieu de montrer ce qui se passe
              (ex : "Leurs deux territoires etaient desormais etablis dans un respect mutuel")
            - conclusions narratorielles : phrases qui expliquent au lecteur ce qu'il doit
              comprendre au lieu de le laisser l'inferer
              (ex : "Le silence n'etait plus un vide mais une forme de coexistence.",
               "Quelque chose avait change entre eux.")
            - langage conceptuel : concepts abstraits utilises la ou une situation, un geste
              ou une perception concrete suffirait
              (ex : "sa presence occupait l'espace", "une dynamique implicite")
            - groupes nominaux artificiels : "le deploiement reflechi du foulard"
              au lieu de "elle deplia son foulard"
            - vocabulaire de planification, d'optimisation ou d'analyse dans une narration
              ordinaire : "optimise", "positionne strategiquement", "effectue une transition"

            Pour chaque passage suspect, pose-toi ces questions :
            - Un romancier publierait-il naturellement cette phrase sans la retoucher ?
            - La meme idee pourrait-elle etre exprimee de facon plus simple, plus concrete
              et plus naturelle sans perte d'information ? Si oui, le passage est
              probablement artificiel.
            - Si la phrase interprete la scene au lieu de la decrire, cette interpretation
              provient-elle clairement du point de vue d'un personnage ? Si non, signale-la.

            FORMAT STRICT — repete ce bloc pour chaque passage artificiel detecte :
            EXTRAIT :
            [citation exacte de la phrase ou du groupe de mots artificiel]

            PROBLEME :
            [description concise du defaut]

            SUGGESTION :
            [reecriture plus naturelle]

            Exemple :
            EXTRAIT :
            Elle noua son foulard dans un geste protecteur et deliberement visible.

            PROBLEME :
            Sur-interpretation narrative : le narrateur attribue une intention au geste
            au lieu de le montrer.

            SUGGESTION :
            Elle noua son foulard.

            Si aucun passage n'est artificiel, ecrire uniquement : [RIEN]
            Rien d'autre : ni introduction ni conclusion.
            En francais.""";

    private static final String AGENT_NAME = "NaturalityFilter";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public NaturalityFilter(ModelCallPort llm) {
        this.llm = llm;
    }

    public NaturalityFilterOutput call(NaturalityFilterInput input) {
        int ctx = llm.contextWindow();
        String user = "### Texte\n" + trunc(input.text(), ctx * 4 * 60 / 100)
            + "\n\nAnalyse la naturalite du texte.";
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName())).text();
        return new NaturalityFilterOutput(parseFindings(raw));
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    private static List<NaturalityFinding> parseFindings(String raw) {
        List<NaturalityFinding> findings = new ArrayList<>();
        if (raw == null) return findings;
        String norm = normalizeKeywords(raw);
        if (norm.replaceAll("\\s+", "").toUpperCase().contains("[RIEN]")) return findings;

        String[] blocks = norm.split("(?i)EXTRAIT\\s*:");
        for (int i = 1; i < blocks.length; i++) {
            NaturalityFinding f = parseBlock(blocks[i]);
            if (f != null) findings.add(f);
        }
        return findings;
    }

    private static NaturalityFinding parseBlock(String block) {
        String upper = block.toUpperCase();
        int pbIdx = upper.indexOf("PROBLEME");
        int sgIdx = upper.indexOf("SUGGESTION");
        if (pbIdx < 0 || sgIdx < 0 || pbIdx >= sgIdx) return null;

        String citation   = stripColon(block.substring(0, pbIdx)).trim();
        String probleme   = stripColon(block.substring(pbIdx + "PROBLEME".length(), sgIdx)).trim();
        String suggestion = stripColon(block.substring(sgIdx + "SUGGESTION".length())).trim();

        return citation.isBlank() ? null : new NaturalityFinding(citation, probleme, suggestion);
    }

    private static String stripColon(String s) {
        String t = s.trim();
        return t.startsWith(":") ? t.substring(1).trim() : t;
    }

    private static String normalizeKeywords(String raw) {
        return raw
            .replaceAll("(?i)EXTRAIT\\s*:",    "EXTRAIT :")
            .replaceAll("(?i)PROBL[EÈ]ME\\s*:", "PROBLEME :")
            .replaceAll("(?i)SUGGESTION\\s*:",  "SUGGESTION :");
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
