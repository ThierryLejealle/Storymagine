package storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.AgentCorrector;
import storymagine.redacteur.coeur.domaine.agent.commun.CorrectionParser;
import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;

import java.util.List;

/**
 * Detects LLM-style unnatural phrasing and provides direct rewrite suggestions.
 * Applied as a post-processing substitution on the final chapter text (like ProofreaderCorrector).
 */
public class NaturalityCorrector implements AgentCorrector {

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

            Cherche en priorite ces 8 signatures :

            1. GESTE SUR-INTERPRETE — une intention ou une fonction est plaquee sur un geste
               qui se suffirait a lui-meme.
               FAUX : "Elle croisa les bras, signalant ainsi son refus de poursuivre la conversation."
               JUSTE : "Elle croisa les bras."

            2. PHRASE-BILAN — la phrase resume ou theorise une situation au lieu de montrer
               ce qui se passe.
               FAUX : "Ils rirent ensemble pour la premiere fois, preuve que la glace etait enfin brisee."
               JUSTE : "Ils rirent ensemble pour la premiere fois."

            3. CONCEPT A LA PLACE DU CONCRET — un mot abstrait remplace une perception ou un etat concret.
               FAUX : "Un sentiment de peur commencait a se manifester chez le vieil homme."
               JUSTE : "Le vieil homme commencait a avoir peur."

            4. ACTION NOMINALISEE — un verbe simple est remplace par un groupe nominal.
               FAUX : "Elle procéda au déploiement réfléchi de son foulard."
               JUSTE : "Elle déplia son foulard."

            5. PRENOM AUTO-REFERENTIEL — le texte emploie le prenom d'un personnage-focalisateur
               la ou un pronom suffirait — artefact qui trahit une gestion par entites plutot que
               par narration organique.
               FAUX : "Lexy tendit sa main vers le medecin. Elle le voyait etudier attentivement
                       les blessures de la main de Lexy."
               JUSTE : "Lexy tendit sa main vers le medecin. Elle le voyait etudier attentivement
                        les blessures de sa main."

            6. VOCABULAIRE DE MACHINE — planification, optimisation ou analyse dans une narration
               ordinaire.
               FAUX : "Elle optimisa la disposition des tasses avant l'arrivee de sa soeur."
               JUSTE : "Elle rangea les tasses avant l'arrivee de sa soeur."

            7. AMBIANCE EXPLIQUEE — le texte nomme directement une atmosphere ou un effet emotionnel
               au lieu de laisser les details concrets le faire ressentir.
               FAUX : "Une atmosphere etrange regnait dans la piece ; les volets claquaient."
               JUSTE : "Les volets claquaient."

            8. REFERENCE DE PERSONNAGE INCOHERENTE — le pronom ou l'article utilise pour un
               personnage change sans raison dans le meme passage (le LLM se perd entre les
               personnages, ce n'est pas une faute de francais mais une perte de contexte).
               FAUX : "Il se figea sur le seuil. L'odeur familiere la frappa aussitot."
               JUSTE : "Il se figea sur le seuil. L'odeur familiere le frappa aussitot."

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
            - FAUX: "Elle croisa les bras, signalant ainsi son refus de poursuivre la conversation."
              JUSTE: "Elle croisa les bras."

            FAUX doit être recopié mot pour mot, en entier — jamais de "..." pour abréger un passage long.
            Ni FAUX ni JUSTE ne portent de commentaire ou de justification après la citation.
              MAUVAIS : - FAUX: "...ce cadre idyllique." (Répétition structurelle)
              BON     : - FAUX: "...ce cadre idyllique."

            NE DENATURE JAMAIS LE TEXTE
            Corrige par remplacement : la phrase JUSTE garde le sens, les faits, les evenements
            et les actions de la phrase FAUX, en modifiant le moins de mots possible.
            Ne resume jamais, ne compresse jamais — et ne change jamais qui fait quoi.

            UNE SEULE suppression est autorisee : le commentaire accroche a une action, qui
            explique son intention, sa signification ou ce qu'elle prouve
            ("signalant ainsi...", "preuve que...", "un geste qui trahissait...").
            Ce commentaire n'est pas une information du recit : c'est l'ecriture qui se commente
            elle-meme. Supprime le commentaire, garde l'action mot pour mot (signatures 1 et 2).
            Cette autorisation ne permet jamais de raccourcir le reste du texte.

            Une seule phrase par ligne JUSTE, sans variante, sans commentaire, sans explication.

            Si aucun passage n'est artificiel : retourner "PAS DE CORRECTION" — rien avant, rien apres.
            Rien d'autre : ni introduction ni conclusion.
            En francais.""";

    private static final String AGENT_NAME = "SequenceNaturalityCorrector";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    @Override
    public RetryStrategy retryStrategy() { return RetryStrategy.RATIO_THRESHOLD; }

    public NaturalityCorrector(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public NaturalityCorrectorOutput call(NaturalityCorrectorInput input) {
        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
            .section("Texte", t.text(input.text(), ctx * 4 * 60 / 100, "text"))
            .raw("Analyse la naturalite du texte.")
            .build();
        t.logIfTruncated(log, agentName());
        String raw = llm.generate(SYSTEM, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new NaturalityCorrectorOutput(parseFindings(raw));
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    private static List<NaturalityFinding> parseFindings(String raw) {
        return CorrectionParser.parse(raw, "PAS DE CORRECTION", NaturalityFinding::new);
    }
}
