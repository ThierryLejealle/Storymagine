package storymagine.redacteur.coeur.domaine.agent.writer.sequencewriter;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.stream.Collectors;

/**
 * Generates prose for a single sequence.
 * Assembles slot-bounded context from WriterInput and calls the LLM.
 * Source: WriterContext.writeSequence.
 */
public class Writer implements Agent {

    private static final String AGENT_NAME = "Writer";

    private final ModelCallPort llm;

    @Override
    public String agentName() { return AGENT_NAME; }

    public Writer(ModelCallPort llm) {
        this.llm = llm;
    }

    public WriterOutput call(WriterInput input) {
        String system = buildSystem(input);
        String user   = buildUser(input);
        String raw = llm.generate(system, user, 0.8, LlmCallContext.of(agentName(), agentLabel())).text();
        return new WriterOutput(raw);
    }

    private String buildSystem(WriterInput in) {
        // 1. Préfixe réécriture (optionnel — apparaît en tête du prompt)
        String rewritePrefix = !in.isRewrite() ? ""
                : "RÉÉCRITURE — Un texte précédent a été jugé insuffisant par les critiques.\n"
                  + "Corrige impérativement les problèmes listés dans la section \"### Problèmes à corriger\" de la trame, avant toute autre considération.\n"
                  + "Chaque problème doit être traité dans ce nouveau texte.\n";

        // 2. Contrainte de longueur (suit le rôle de base)
        String lengthConstraint = in.minWords() > 0
                ? "Chaque séquence fait au minimum " + in.minWords() + " mots — développe les scènes avec profondeur et précision sensorielles."
                : "Chaque séquence fait entre 300 et 800 mots.";

        // 3. Règle d'ouverture selon présence du texte précédent (suite de la contrainte de longueur)
        boolean hasPrev = in.prevSentences() != null && !in.prevSentences().isBlank();
        String openingRule = (in.stitch() != null && !in.stitch().isBlank()) ? in.stitch()
                : hasPrev
                        ? "Raccorde au texte précédent sans le résumer ni le paraphraser — "
                          + "ces phrases sont déjà écrites, ta première phrase est la suivante dans le récit : poursuis l'action. "
                          + "Ne repose pas le décor ni l'ambiance générale déjà établis — "
                          + "sauf si la trame te le demande explicitement."
                        : "La première phrase de chaque séquence utilise un point d'entrée "
                          + "différent de la séquence précédente : action physique, fragment de dialogue, "
                          + "détail sensoriel, pensée intérieure, objet ou geste isolé. "
                          + "Ne commence ni par le prénom d'un personnage seul ni par un pronom sujet nu "
                          + "('Il', 'Elle', 'Ils') — montre d'abord ce qui se passe, le personnage en est le sujet implicite.";

        return rewritePrefix
                + """
                Tu es un écrivain littéraire.
                Tu suis la trame générale dans l'ordre indiqué — chaque élément de la trame DOIT apparaître dans le texte.
                Tu peux enrichir la scène avec des actions, réactions, observations ou dialogues locaux —
                à condition de ne pas modifier les éléments de la trame, les relations entre personnages ni l'issue de la scène.
                La trame est découpée en séquences numérotées : checks, contraintes, focus ou lore propres à une séquence ne s'appliquent qu'à cette séquence.
                Tu respectes intégralement les directives détaillées de l'auteur pour cette séquence :
                elles précisent et enrichissent la trame, et ont priorité sur elle si les deux divergent.
                Tu ne prends aucune décision narrative : ton seul rôle est de transformer ces instructions en prose française de haute qualité.
                Tu ne produis QUE le texte narratif — aucun commentaire, aucun méta-texte.
                En cas de conflit entre les instructions :
                1. Directives détaillées de l'auteur
                2. Contraintes de rédaction
                3. Éléments de la trame (dans l'ordre)
                4. Fiches personnages
                5. Style et exemple de rédaction
                6. Focus
                7. Lore
                """
                + lengthConstraint + "\n" + openingRule + "\n"
                + """
                Tu n'écris que les événements de cette séquence —
                aucune allusion aux séquences suivantes, aucun indice sur la suite,
                aucune anticipation des événements à venir.
                Si la fiche d'un personnage précise un article et un pronom
                (ligne 'Article : X — pronom'), respecte-les strictement dans tout le texte.
                Les traits visibles d'un personnage (tenue, apparence physique, gestes récurrents)
                et son tempérament sont des faits non négociables.
                Ne cite ni ne paraphrase la fiche — incarne ces traits dans la prose.""";
    }

    private String buildUser(WriterInput in) {
        int ctx  = llm.contextWindow();

        // Slot sizes (chars)
        int sState       = ctx * 4 / 16;
        int sJournal     = ctx * 4 / 20;
        int sActions     = ctx * 4 / 20;
        int sFocus       = ctx * 4 / 16;
        int sLore        = ctx * 4 / 16;
        int sConstraints = ctx * 4 / 16;
        int sStyleGuide  = ctx * 4 / 20;
        int sChars       = ctx * 4 / 8;
        int sPlan        = ctx * 4 / 8;
        int sHistory     = ctx * 4 / 8;
        int sExample     = ctx * 4 / 16;

        // Interdictions en tête du user, proches de la génération
        String bannedPhrases = in.forbiddenPhrases().isEmpty() ? ""
                : "EXPRESSIONS À NE PAS RÉPÉTER (déjà utilisées dans ce livre) :\n"
                  + in.forbiddenPhrases().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        String bannedThemes = in.forbiddenThemes().isEmpty() ? ""
                : "SCHÉMAS NARRATIFS USÉS — à reformuler différemment ou à omettre :\n"
                  + in.forbiddenThemes().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));

        StringBuilder sb = new StringBuilder();
        if (!bannedPhrases.isBlank()) sb.append(bannedPhrases).append("\n\n");
        if (!bannedThemes.isBlank())  sb.append(bannedThemes).append("\n\n");
        appendSection(sb, "État actuel des entités",        trunc(in.entityState(),          sState));
        appendSection(sb, "Itérations précédentes (journal)", trunc(in.loopJournal(),        sJournal));
        appendSection(sb, "Action narrative à explorer",    trunc(in.actionsText(),          sActions));
        String fWrite = trunc(in.focusText(), sFocus);
        appendSection(sb, "Éléments à utiliser (focus)",
            fWrite.isBlank() ? "" : "Efforce-toi d'utiliser ces éléments dans l'histoire que tu vas rédiger.\n" + fWrite);
        String lWrite = trunc(in.loreText(), sLore);
        appendSection(sb, "Informations utiles (lore)",
            lWrite.isBlank() ? "" : "N'hésite pas à piocher dans ces informations pour étoffer la rédaction.\n" + lWrite);
        appendSection(sb, "Règles de rédaction",            trunc(in.redactionConstraints(), sConstraints));
        appendSection(sb, "Guide de style",                 trunc(in.styleGuide(),           sStyleGuide));
        appendSection(sb, "Personnages présents",           trunc(in.charactersText(),       sChars));
        appendSection(sb, "Trame générale (ordre et structure)", trunc(in.chapterPlan(),     sPlan));
        appendSection(sb, "Histoire jusqu'ici (résumé)",
            lastSentences(in.storySoFar(), sHistory * 4));
        appendSection(sb, "Exemple du style attendu",       trunc(in.writingExample(),       sExample));

        if (in.sequenceContext() != null && !in.sequenceContext().isBlank())
            sb.append("\n\n[Contexte de la séquence]\n").append(in.sequenceContext());
        if (in.setting() != null && !in.setting().isBlank())
            sb.append("\n\n[Cadre de la scène]\n").append(in.setting());

        String directiveBlock = (in.sequencePlan() != null && !in.sequencePlan().isBlank())
            ? in.sequencePlan()
            : (in.sequenceDescription() != null ? in.sequenceDescription() : "");
        if (!directiveBlock.isBlank())
            sb.append("\n\n### Directives détaillées (prioritaires)\n").append(directiveBlock);

        if (in.prevSentences() != null && !in.prevSentences().isBlank()) {
            sb.append("\n\n### Texte précédent — DÉJÀ ÉCRIT, ne pas reproduire\n« ")
              .append(in.prevSentences().strip())
              .append(" »\nNe reproduis pas ce texte — ta première phrase DOIT en être la suite directe.");
        }

        sb.append("\n\nÉcris le texte de cette séquence en respectant intégralement ces directives.");
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, String content) {
        if (content == null || content.isBlank()) return;
        sb.append("\n\n### ").append(title).append("\n").append(content);
    }

    private static String lastSentences(String text, int maxChars) {
        if (text == null || text.isBlank()) return "";
        if (text.length() <= maxChars) return text;
        String tail = text.substring(text.length() - maxChars);
        int dot = tail.indexOf('.');
        return dot >= 0 ? tail.substring(dot + 1).trim() : tail.trim();
    }

    private static String trunc(String s, int maxChars) {
        if (s == null || s.isBlank()) return "";
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "…";
    }
}
