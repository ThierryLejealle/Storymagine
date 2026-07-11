package storymagine.redacteur.coeur.domaine.agent.sequence.writer;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.SummaryBudget;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;

import java.util.stream.Collectors;

/**
 * Generates prose for a single sequence.
 * Assembles slot-bounded context from WriterInput and calls the LLM.
 * Source: WriterContext.writeSequence.
 */
public class Writer implements Agent {

    private static final String AGENT_NAME = "SequenceWriter";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public Writer(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public WriterOutput call(WriterInput input) {
        String system = buildSystem(input);
        String user   = buildUser(input);
        String raw = llm.generate(system, user, 0.8, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new WriterOutput(raw);
    }

    private String buildSystem(WriterInput in) {
        // 1. Préfixe réécriture (optionnel — apparaît en tête du prompt)
        String rewritePrefix = !in.isRewrite() ? ""
                : "RÉÉCRITURE — Un texte précédent a été jugé insuffisant par les critiques.\n"
                  + "Corrige impérativement les problèmes listés dans la section \"### Problèmes à corriger\" des directives, avant toute autre considération.\n"
                  + "Chaque problème doit être traité dans ce nouveau texte.\n";

        // 2. Contrainte de longueur (suit le rôle de base) — qualité toujours exigée, seul le seuil chiffré varie
        String lengthConstraint = "Développe la scène avec profondeur et précision sensorielles. "
                + (in.minWords() > 0
                    ? "Cette séquence fait au minimum " + in.minWords() + " mots."
                    : "Cette séquence fait entre 300 et 800 mots.");

        // 3. Règle d'ouverture : stitch (consigne explicite de transition) > continuité par défaut > point d'entrée varié
        boolean hasStitch = in.stitch() != null && !in.stitch().isBlank();
        boolean hasPrev   = in.previousSequenceText() != null && !in.previousSequenceText().isBlank();
        String openingRule = hasStitch
                ? "Applique cette consigne pour gérer le passage de la séquence précédente à celle que tu vas écrire : "
                  + in.stitch()
                : hasPrev
                        ? "Raccorde au texte précédent sans le résumer ni le paraphraser — "
                          + "ces phrases sont déjà écrites, ta première phrase est la suivante dans le récit : poursuis l'action. "
                          + "Ne repose pas le décor ni l'ambiance générale déjà établis — "
                          + "sauf si les directives te le demandent explicitement."
                        : "La première phrase de cette séquence utilise un point d'entrée varié : "
                          + "action physique, fragment de dialogue, détail sensoriel, pensée intérieure, objet ou geste isolé. "
                          + "Ne commence ni par le prénom d'un personnage seul ni par un pronom sujet nu "
                          + "('Il', 'Elle', 'Ils') — montre d'abord ce qui se passe, le personnage en est le sujet implicite.";

        return rewritePrefix
                + """
                Tu es un écrivain littéraire.
                Tu suis les directives détaillées de l'auteur pour cette séquence, dans l'ordre indiqué —
                chaque élément qu'elles décrivent DOIT apparaître dans le texte.
                Tu peux enrichir la scène avec des actions, réactions, observations ou dialogues locaux —
                à condition de ne pas modifier les éléments demandés, les relations entre personnages ni l'issue de la scène.
                Tu ne produis QUE le texte narratif — aucun commentaire, aucun méta-texte.
                En cas de conflit entre les instructions suivantes :
                1. Directives détaillées de l'auteur
                2. Contraintes de rédaction
                3. Fiches personnages
                4. Guide de style
                5. Exemple de rédaction
                6. Focus
                7. Lore
                Les expressions interdites, les schémas narratifs à éviter, l'état des entités déjà établi
                et le texte précédent ne sont jamais soumis à cet arbitrage : ce sont des faits ou des
                interdits absolus, toujours respectés quoi qu'il arrive.
                """
                + lengthConstraint + "\n" + openingRule + "\n"
                + """
                Tu n'écris que les événements de cette séquence — aucune anticipation des événements des séquences suivantes.
                Si la fiche d'un personnage précise un article et un pronom (ligne 'Article : X — pronom'),
                respecte-les strictement dans tout le texte. Les traits visibles d'un personnage (tenue,
                apparence physique, gestes récurrents) et son tempérament sont des faits non négociables —
                ne cite ni ne paraphrase la fiche, incarne ces traits dans la prose.
                Le texte ne doit pas donner l'impression d'avoir été rédigé par une IA : privilégie un
                vocabulaire et des tournures usuels, évite les formulations qui sonnent artificiel. Cette
                règle de naturel s'efface uniquement devant une consigne contraire explicite du guide de style.
                """;
    }

    private String buildUser(WriterInput in) {
        int ctx  = llm.contextWindow();

        // Slot sizes (chars)
        int sState       = ctx * 4 / 16;
        int sFocus       = ctx * 4 / 16;
        int sLore        = ctx * 4 / 16;
        int sConstraints = ctx * 4 / 16;
        int sStyleGuide  = ctx * 4 / 20;
        int sChars       = ctx * 4 / 8;
        // Même fraction que le seuil de compaction du résumé global (StorySummaryStep) —
        // voir SummaryBudget. TODO(budget) : 1/8 est peut-être trop conservateur pour un
        // résumé de tout le livre — à étudier plus tard (voir mémoire "budget Writer trop timide ?").
        int sHistory     = SummaryBudget.charBudget(ctx);
        int sExample     = ctx * 4 / 16;

        String bannedPhrasesList = in.forbiddenPhrases().isEmpty() ? ""
                : in.forbiddenPhrases().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        String bannedThemesList = in.forbiddenThemes().isEmpty() ? ""
                : in.forbiddenThemes().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));

        TruncHelper t = TruncHelper.create();
        String fWrite = t.blockList(in.focusText(), sFocus, "focusText");
        String lWrite = t.blockList(in.loreText(), sLore, "loreText");
        String redactionConstraints = t.list(in.redactionConstraints(), sConstraints, "redactionConstraints");

        // Ordre = inverse de la liste de priorité du system prompt (§ conflit) : le moins prioritaire
        // (Lore, 7) en tête, le plus prioritaire (Directives, 1) juste avant l'instruction finale —
        // effet de récence, déterminant pour un petit LLM. Interdictions/faits établis restent en
        // tête : ils ne sont jamais mis en balance (cf. system prompt), pas de position à optimiser.
        PromptBuilder pb = PromptBuilder.create()
            .section("Expressions à ne pas répéter (déjà utilisées dans ce livre)", bannedPhrasesList)
            .section("Schémas narratifs usés — à reformuler différemment ou à omettre", bannedThemesList)
            .section("État actuel des entités",     t.list(in.entityState(), sState, "entityState"))
            .section("Histoire jusqu'ici (résumé)", t.tailText(in.summary(), sHistory, "summary"))
            .section("Lore",
                lWrite.isBlank() ? "" : "N'hésite pas à piocher dans ces informations pour étoffer la rédaction.\n" + lWrite)
            .section("Focus",
                fWrite.isBlank() ? "" : "Efforce-toi d'utiliser ces éléments dans l'histoire que tu vas rédiger.\n" + fWrite)
            .section("Exemple de rédaction",        t.text(in.writingExample(), sExample,    "writingExample"))
            .section("Guide de style",              t.text(in.styleGuide(),     sStyleGuide, "styleGuide"))
            .section("Fiches personnages",          t.blockList(in.charactersText(), sChars, "charactersText"))
            .section("Contraintes de rédaction",
                redactionConstraints.isBlank() ? "" : "Assure-toi que chacun des points suivants est respecté :\n" + redactionConstraints)
            .section("Cadre de la scène", in.setting());

        String directiveBlock = (in.sequencePlan() != null && !in.sequencePlan().isBlank())
            ? in.sequencePlan()
            : (in.sequenceDescription() != null ? in.sequenceDescription() : "");
        if (in.rewriteProblems() != null && !in.rewriteProblems().isBlank()) {
            directiveBlock += "\n\n### Problèmes à corriger\n" + in.rewriteProblems();
        }
        pb.section("Directives détaillées de l'auteur", directiveBlock);

        boolean hasStitch = in.stitch() != null && !in.stitch().isBlank();
        if (in.previousSequenceText() != null && !in.previousSequenceText().isBlank()) {
            String note = hasStitch
                ? "« " + in.previousSequenceText().strip() + " »\nCe texte est déjà écrit — ne le reproduis pas."
                : "« " + in.previousSequenceText().strip() + " »\n"
                  + "Ne reproduis pas ce texte — ta première phrase DOIT en être la suite directe.";
            pb.section("Texte précédent — DÉJÀ ÉCRIT, ne pas reproduire", note);
        }

        pb.raw("Écris le texte de cette séquence en respectant intégralement l'ensemble des éléments ci-dessus.");
        t.logIfTruncated(log, agentName());
        return pb.build();
    }
}
