package storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.SummaryBudget;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Produces a chapter plan — either a free-form narrative outline or a structured JSON
 * array (one object per sequence) depending on {@code input.jsonMode()}.
 * In correction mode ({@code input.previousPlan()} non-null), fixes a rejected plan.
 * Source: ScenarioPlannerContext (planInternal + planChapterWithCorrection).
 */
public class ChapterPlanner implements Agent {

    private static final String JSON_PLANNER_SYSTEM = """
            Tu es le planificateur de scènes d'un roman.
            Tu transformes chaque séquence en un plan de scène détaillé.

            Le chapitre est découpé en séquences indépendantes. Planifie-les toutes, dans l'ordre,
            sans déborder d'une séquence sur la suivante.

            Ta sortie est un tableau JSON strictement valide, sans markdown, sans commentaire,
            sans texte avant ou après.

            Format obligatoire :
            [
              {
                "sequence": <numéro entier>,
                "beats": ["<beat 1>", "<beat 2>", "..."],
                "sensoriels": "<sons, lumières, textures, odeurs, températures, mouvements — uniquement des éléments perceptibles>",
                "intention_de_scene": "<effet recherché sur le lecteur — maximum 15 mots, pas de prose ni de métaphore>"
              }
            ]

            Règles pour les beats, par ordre de priorité :
                1. Couvre TOUS les éléments de la consigne, sans exception.
                2. Développe uniquement les événements déjà présents en ajoutant des gestes, réactions, déplacements, dialogues ou détails sensoriels.
                3. Ne modifie jamais les faits importants de la séquence, les personnages présents, leurs relations ou son issue.
                4. Les éléments abstraits des instructions (intentions, objectifs narratifs, formulations) sont des objectifs, pas des beats. Traduis-les toujours en événements observables : gestes, paroles, réactions visibles ou détails sensoriels.
                5. Chaque beat doit montrer un fait observable. N'écris jamais d'interprétation, de conclusion ou de concept abstrait.
                6. Ne dépasse jamais la fin de la séquence. Si un événement marque sa fin, le dernier beat s'arrête exactement à cet événement.
            
            Nombre de beats par séquence :
            - Le nombre attendu est fourni dans la description de chaque séquence.
            - Choisis le découpage le plus naturel dans cette plage.
            - Augmente le nombre lorsque plusieurs événements importants devraient sinon être regroupés.
            - Réduis le nombre lorsqu'il faudrait découper artificiellement une même action en plusieurs beats.

            Contraintes sur les beats :
            - Chaque beat décrit un seul moment narratif. Il peut contenir plusieurs actions lorsqu'elles appartiennent au même mouvement.
            - Chaque beat fait progresser la scène ou développe naturellement un moment déjà engagé. Ne découpe pas artificiellement une même action.
            - Test : une caméra doit pouvoir montrer le beat.
            - Interdit : thèmes, symboles, interprétations, conclusions, états relationnels, concepts abstraits.
            - Évite les formulations qui expliquent la scène au lieu de la montrer. Lorsque les deux sont possibles, préfère un fait observable à son interprétation.

            Exemples (abstrait → observable) :
            - "Le silence s'installe."               →  "Personne ne parle pendant plusieurs secondes."
            - "Une proximité naît entre eux."        →  "Leurs épaules se rapprochent légèrement."
            - "La tension monte."                    →  "Après quelques secondes, Maya répond enfin."
            - "Le courant passe."                    →  "Leurs regards se croisent ; aucun ne détourne immédiatement les yeux."
            - "Une coexistence silencieuse s'établit." →  "Eddie tourne une page pendant que Maya regarde les champs."
            
            En français.""";

    private static final String FREE_PLANNER_SYSTEM = """
            Tu es un architecte narratif. Ton rôle : décider avec précision CE QUI SE PASSE dans ce chapitre.
            Produis un plan impératif — chaque point est une instruction que le rédacteur DOIT exécuter à la lettre.
            Structure : événements dans l'ordre chronologique, réactions des personnages, tournant dramatique, fin du chapitre.
            Ne t'occupe pas de la prose — seulement de ce qui se passe, pourquoi, et dans quel ordre.
            En français.""";

    private static final String CORRECTION_SYSTEM = """
            Tu es le planificateur de scènes d'un roman. Tu dois corriger un plan JSON existant.
            Corrige impérativement les problèmes listés — chaque problème doit être traité.
            Pour les séquences non concernées par un problème, conserve les beats à l'identique.
            Produis un tableau JSON strictement valide, même format que le plan précédent,
            sans markdown, sans commentaire, sans texte avant ou après.
            En français.""";

    private static final String INNER_STATE_NOTE = """

            Si des éléments intérieurs ([État intérieur]) sont fournis,
            ils peuvent orienter les tensions et tournants narratifs —
            ne pas les mentionner explicitement dans le plan produit.""";

    private static final String FOCUS_LORE_NOTE = """
    
            Focus et lore : la section « Éléments à utiliser (focus) — toutes les séquences »
            s'applique globalement à l'ensemble du chapitre — intègre ces éléments dans les séquences où ils s'y prêtent naturellement sans faire de redit direct.
            La section « Informations utiles (lore) — toutes les séquences » s'applique à l'ensemble du chapitre
            — utilise ces informations lorsqu'elles enrichissent naturellement la séquence.
            Attention : chaque séquence peut aussi avoir ses propres focus, lore et contraintes
            — ceux-ci s'appliquent uniquement à cette séquence, pas aux autres.""";

    private static final String AGENT_NAME = "ChapterPlanner";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterPlanner(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public ChapterPlannerOutput call(ChapterPlannerInput input) {
        boolean isCorrection = input.previousPlan() != null && !input.previousPlan().isBlank();
        String systemPrompt = buildSystem(input, isCorrection);
        String userPrompt   = buildUser(input, isCorrection);
        String raw = llm.generate(systemPrompt, userPrompt, 0.7, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        List<String> seqPlans = input.jsonMode() ? parseJsonSequences(raw, input.sequenceDescriptions()) : List.of();
        return new ChapterPlannerOutput(raw, seqPlans);
    }

    private String buildSystem(ChapterPlannerInput in, boolean isCorrection) {
        if (isCorrection) {
            return CORRECTION_SYSTEM + "\n" + FOCUS_LORE_NOTE + buildForbiddenPhrases(in);
        }

        // Préfixe de réécriture commun aux deux modes (JSON et libre)
        String rewritePrefix = !in.isRewrite() ? ""
                : "RÉVISION — Un précédent plan a été jugé insuffisant.\n"
                  + "Tu dois corriger impérativement les problèmes listés avant toute autre considération.\n";

        String mainSystem = in.jsonMode() ? JSON_PLANNER_SYSTEM : FREE_PLANNER_SYSTEM;

        return rewritePrefix + mainSystem + "\n" + INNER_STATE_NOTE + "\n" + FOCUS_LORE_NOTE
                + buildForbiddenPhrases(in);
    }

    private static String buildForbiddenPhrases(ChapterPlannerInput in) {
        if (in.forbiddenPhrases().isEmpty()) return "";
        return "\nÉVITE ces formulations déjà utilisées dans les chapitres précédents :\n"
                + in.forbiddenPhrases().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
    }

    private String buildUser(ChapterPlannerInput in, boolean isCorrection) {
        int ctx = llm.contextWindow();

        TruncHelper t = TruncHelper.create();
        String constraints = t.list(in.constraints(), ctx * 4 / 12, "constraints");
        String fPlan = t.blockList(in.focusText(), ctx * 4 / 12, "focusText");
        String lPlan = t.blockList(in.loreText(), ctx * 4 / 12, "loreText");

        PromptBuilder pb = PromptBuilder.create()
            .section("Objectif du livre",          t.text(in.bookGoal(),    ctx * 4 / 16, "bookGoal"))
            .section("Personnages de ce chapitre", t.blockList(in.characters(),  ctx * 4 / 8, "characters"))
            .section("Contraintes",
                constraints.isBlank() ? "" : "Assure-toi que chacun des points suivants est respecté :\n" + constraints)
            .section("Éléments à utiliser (focus) — toutes les séquences", fPlan)
            .section("Informations utiles (lore) — toutes les séquences", lPlan)
            .section("État actuel des entités",    t.list(in.entityState(), ctx * 4 / 12, "entityState"))
            .section("Histoire jusqu'ici",         t.text(in.summary(),  SummaryBudget.charBudget(ctx), "summary"));

        StringBuilder chapterBlock = new StringBuilder();
        chapterBlock.append("### Chapitre à planifier\n")
                    .append("Titre : ").append(in.chapterTitle()).append("\n");
        if (in.chapterSetting() != null && !in.chapterSetting().isBlank())
            chapterBlock.append("Cadre : ").append(in.chapterSetting()).append("\n");
        if (in.chapterDescription() != null && !in.chapterDescription().isBlank())
            chapterBlock.append("Description : ").append(in.chapterDescription()).append("\n");
        appendSequencesBlock(chapterBlock, in.sequenceDescriptions());
        pb.raw(chapterBlock.toString());

        if (isCorrection && in.previousPlan() != null) {
            pb.section("Plan précédent (à corriger)", t.text(in.previousPlan(), ctx * 4 / 4, "previousPlan"));
        }

        if (in.problemsToFix() != null && !in.problemsToFix().isBlank()) {
            String instruction = isCorrection
                ? in.problemsToFix() + "\n\nCorrige chacun des points ci-dessus. Produis le plan corrigé au format JSON."
                : in.problemsToFix();
            pb.section("Problèmes à " + (isCorrection ? "corriger" : "éviter") + " impérativement", instruction);
        }

        if (in.jsonMode() && !in.sequenceDescriptions().isEmpty()) {
            pb.raw("Produis exactement " + in.sequenceDescriptions().size()
                + " objet(s) dans le tableau JSON — un par séquence dans l'ordre.");
        }
        t.logIfTruncated(log, agentName());
        return pb.build();
    }

    private void appendSequencesBlock(StringBuilder sb, List<String> seqDescriptions) {
        if (seqDescriptions == null || seqDescriptions.isEmpty()) {
            sb.append("\nPas de séquences prédéfinies — organise le chapitre en scènes narratives.\n");
            return;
        }
        sb.append("\nSéquences à couvrir dans l'ordre (").append(seqDescriptions.size()).append(") :\n");
        for (int i = 0; i < seqDescriptions.size(); i++) {
            sb.append(i + 1).append(". ").append(seqDescriptions.get(i)).append("\n\n");
        }
    }

    // Minimal JSON array parser — extracts beats/sensoriels/ton_et_rythme per sequence.
    // Avoids Jackson in coeur; handles the fixed format the JSON_PLANNER_SYSTEM produces.
    private List<String> parseJsonSequences(String raw, List<String> seqDescriptions) {
        List<String> result = new ArrayList<>();
        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');
        if (start < 0 || end <= start) return result;
        String json = raw.substring(start, end + 1);

        // Split on top-level object boundaries
        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = json.substring(objStart, i + 1);
                    int seqNum = extractInt(obj, "sequence");
                    String beats          = extractBeats(obj);
                    String sensoriels     = extractString(obj, "sensoriels");
                    String intentionScene = extractString(obj, "intention_de_scene");
                    String directive      = (seqDescriptions != null && seqNum >= 1 && seqNum <= seqDescriptions.size())
                        ? seqDescriptions.get(seqNum - 1) : "";
                    result.add(formatForWriter(directive, beats, sensoriels, intentionScene));
                    objStart = -1;
                }
            }
        }
        return result;
    }

    private static String formatForWriter(String directive, String beats, String sensoriels, String intentionScene) {
        StringBuilder sb = new StringBuilder();
        if (directive != null && !directive.isBlank())
            sb.append("Consigne : ").append(directive.trim()).append("\n\n");
        if (beats != null && !beats.isBlank())
            sb.append("BEATS :\n").append(beats).append("\n");
        if (sensoriels != null && !sensoriels.isBlank())
            sb.append("\nSENSORIELS : ").append(sensoriels).append("\n");
        if (intentionScene != null && !intentionScene.isBlank())
            sb.append("\nINTENTION : ").append(intentionScene).append("\n");
        return sb.toString().trim();
    }

    private static int extractInt(String obj, String key) {
        String pattern = "\"" + key + "\"";
        int ki = obj.indexOf(pattern);
        if (ki < 0) return 0;
        int colon = obj.indexOf(':', ki + pattern.length());
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < obj.length() && Character.isWhitespace(obj.charAt(start))) start++;
        int end = start;
        while (end < obj.length() && Character.isDigit(obj.charAt(end))) end++;
        try { return Integer.parseInt(obj.substring(start, end)); } catch (NumberFormatException e) { return 0; }
    }

    private static String extractString(String obj, String key) {
        String pattern = "\"" + key + "\"";
        int ki = obj.indexOf(pattern);
        if (ki < 0) return "";
        int colon = obj.indexOf(':', ki + pattern.length());
        if (colon < 0) return "";
        int q1 = obj.indexOf('"', colon + 1);
        if (q1 < 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = q1 + 1; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '\\' && i + 1 < obj.length()) { sb.append(obj.charAt(i + 1)); i++; }
            else if (c == '"') break;
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String extractBeats(String obj) {
        int arrStart = obj.indexOf("[", obj.indexOf("\"beats\""));
        if (arrStart < 0) return "";
        int arrEnd = obj.indexOf("]", arrStart);
        if (arrEnd < 0) return "";
        String beatsRaw = obj.substring(arrStart + 1, arrEnd);
        List<String> beats = new ArrayList<>();
        int i = 0;
        while (i < beatsRaw.length() && beats.size() < 50) {
            int q1 = beatsRaw.indexOf('"', i);
            if (q1 < 0) break;
            StringBuilder sb = new StringBuilder();
            boolean closed = false;
            for (int j = q1 + 1; j < beatsRaw.length(); j++) {
                char c = beatsRaw.charAt(j);
                if (c == '\\' && j + 1 < beatsRaw.length()) { sb.append(beatsRaw.charAt(j + 1)); j++; }
                else if (c == '"') { i = j + 1; closed = true; break; }
                else sb.append(c);
            }
            if (!closed) break;  // JSON malformé — guillemet fermante manquante, i non mis à jour
            String beat = sb.toString().trim();
            if (!beat.isBlank()) beats.add(beat);
        }
        StringBuilder result = new StringBuilder();
        for (int n = 0; n < beats.size(); n++) result.append(n + 1).append(". ").append(beats.get(n)).append("\n");
        return result.toString();
    }

}
