package storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner;

import storymagine.commun.coeur.ports.LlmCallContext;
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
            Tu es le planificateur de scènes d'un roman. Ton objectif premier est d'enrichir le contenu par rapport
            à la consigne tout en la respectant. Donne beaucoup plus de détails que la consigne d'origine —
            si elle est courte, c'est sur ta créativité qu'on compte.

            Le chapitre est découpé en séquences indépendantes. Planifie-les toutes, dans l'ordre,
            sans déborder d'une séquence sur la suivante.

            Ta sortie est un tableau JSON strictement valide, sans markdown, sans commentaire,
            sans texte avant ou après.

            Format obligatoire :
            [
              {
                "sequence": <numéro entier>,
                "beats": ["<beat 1>", "<beat 2>", "..."],
                "sensoriels": "<sons, textures, odeurs, lumière, températures, mouvements visibles — pas d'émotions ni d'interprétations>",
                "intention_de_scene": "<effet recherché sur le lecteur — maximum 15 mots, pas de prose ni de métaphore>"
              }
            ]

            Règles pour les beats, par ordre de priorité :
            1. Couvre TOUS les éléments de la consigne sans exception — la couverture est absolue.
            2. Enrichis en développant les événements déjà présents — par des gestes, réactions, déplacements, détails matériels, sons ou dialogues. N'ajoute pas de nouvel événement significatif. Jamais par des interprétations ou des conclusions.
            3. Les éléments abstraits fournis dans les instructions (intentions, formulations, objectifs narratifs) décrivent un but à atteindre — ne les recopie jamais en beat. Traduis-les en événements observables : gestes, paroles, réactions visibles, détails sensoriels.

            Nombre de beats par séquence :
            Le nombre attendu est fourni dans la description de chaque séquence.
            Choisis un nombre qui produit le découpage le plus naturel.
            Augmente le nombre lorsque plusieurs événements importants devraient sinon être regroupés.
            Réduis le nombre lorsqu'il faudrait découper artificiellement une même action en plusieurs beats.

            Contraintes sur les beats :
            - Chaque beat décrit un seul événement. Il peut s'agir d'une action ; d'une réaction visible ; d'une parole ; d'une observation sensorielle.
            - Chaque beat doit apporter une information nouvelle — ne découpe pas une même action en plusieurs beats.
            - Test : une caméra doit pouvoir montrer le beat sans narration explicative.
            - Interdit : thèmes, symboles, interprétations, conclusions, états relationnels, concepts abstraits.
            - Verbes-signal d'abstraction : devenir, symboliser, représenter, exprimer, illustrer, traduire, incarner, installer, établir. Ces verbes sont un signal d'alerte — utilise-les uniquement lorsqu'ils décrivent un fait concret, jamais une interprétation de la scène.
            - Lorsqu'une idée peut être exprimée par un concept ou par un comportement observable, choisir le comportement.

            Exemples (abstrait → observable) :
            "Le silence s'installe."               →  "Personne ne parle pendant plusieurs secondes."
            "Une proximité naît entre eux."        →  "Leurs épaules se rapprochent légèrement."
            "La tension monte."                    →  "Maya hésite avant de répondre."
            "Le courant passe."                    →  "Ils se sourient presque en même temps."
            "Une coexistence silencieuse s'établit." →  "Eddie tourne une page pendant que Maya regarde les champs."
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
            — n'hésite pas à utiliser ces informations librement pour étoffer les séquences lorsque cela est pertinent et naturel.
            Attention : chaque séquence peut aussi avoir ses propres focus, lore et contraintes
            — ceux-ci s'appliquent uniquement à cette séquence, pas aux autres.""";

    private static final String AGENT_NAME = "ChapterPlanner";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public ChapterPlanner(ModelCallPort llm) {
        this.llm = llm;
    }

    public ChapterPlannerOutput call(ChapterPlannerInput input) {
        boolean isCorrection = input.previousPlan() != null && !input.previousPlan().isBlank();
        String systemPrompt = buildSystem(input, isCorrection);
        String userPrompt   = buildUser(input, isCorrection);
        String raw = llm.generate(systemPrompt, userPrompt, 0.7, LlmCallContext.of(agentName())).text();
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
        int half = ctx * 4 / 2; // half context in chars (~4 chars/token)

        StringBuilder sb = new StringBuilder();
        appendSection(sb, "Objectif du livre",          trunc(in.bookGoal(),    ctx * 4 / 16),  half);
        appendSection(sb, "Personnages de ce chapitre", trunc(in.characters(),  ctx * 4 / 8),   half);
        appendSection(sb, "Contraintes",                trunc(in.constraints(), ctx * 4 / 12),  half);
        String fPlan = trunc(in.focusText(), ctx * 4 / 12);
        appendSection(sb, "Éléments à utiliser (focus) — toutes les séquences", fPlan, half);
        String lPlan = trunc(in.loreText(), ctx * 4 / 12);
        appendSection(sb, "Informations utiles (lore) — toutes les séquences", lPlan, half);
        appendSection(sb, "État actuel des entités",    trunc(in.entityState(), ctx * 4 / 12),  half);
        appendSection(sb, "État de cohérence",          trunc(in.coherence(),   ctx * 4 / 12),  half);
        appendSection(sb, "Histoire jusqu'ici",         trunc(in.storySoFar(),  ctx * 4 / 8),   half);

        sb.append("\n\n### Chapitre à planifier\n")
          .append("Titre : ").append(in.chapterTitle()).append("\n");
        if (in.chapterSetting() != null && !in.chapterSetting().isBlank())
            sb.append("Cadre : ").append(in.chapterSetting()).append("\n");
        if (in.chapterDescription() != null && !in.chapterDescription().isBlank())
            sb.append("Description : ").append(in.chapterDescription()).append("\n");
        appendSequencesBlock(sb, in.sequenceDescriptions());

        if (in.problemsToFix() != null && !in.problemsToFix().isBlank()) {
            sb.append("\n\n### Problèmes à ")
              .append(isCorrection ? "corriger" : "éviter")
              .append(" impérativement\n")
              .append(in.problemsToFix());
        }

        if (isCorrection && in.previousPlan() != null) {
            sb.append("\n\n### Plan précédent (à corriger)\n")
              .append(trunc(in.previousPlan(), ctx * 4 / 4))
              .append("\n\nCorrige chacun des problèmes listés. Produis le plan corrigé au format JSON.");
        }

        if (in.jsonMode() && !in.sequenceDescriptions().isEmpty()) {
            sb.append("\n\nProduis exactement ").append(in.sequenceDescriptions().size())
              .append(" objet(s) dans le tableau JSON — un par séquence dans l'ordre.");
        }
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, String content, int budget) {
        if (content == null || content.isBlank()) return;
        sb.append("\n\n### ").append(title).append("\n").append(content);
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
        while (i < beatsRaw.length()) {
            int q1 = beatsRaw.indexOf('"', i);
            if (q1 < 0) break;
            StringBuilder sb = new StringBuilder();
            for (int j = q1 + 1; j < beatsRaw.length(); j++) {
                char c = beatsRaw.charAt(j);
                if (c == '\\' && j + 1 < beatsRaw.length()) { sb.append(beatsRaw.charAt(j + 1)); j++; }
                else if (c == '"') { i = j + 1; break; }
                else sb.append(c);
            }
            String beat = sb.toString().trim();
            if (!beat.isBlank()) beats.add(beat);
        }
        StringBuilder result = new StringBuilder();
        for (int n = 0; n < beats.size(); n++) result.append(n + 1).append(". ").append(beats.get(n)).append("\n");
        return result.toString();
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
