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
                : "REWRITE — A previous draft was judged insufficient by the critics.\n"
                  + "You must fix the problems listed in the \"### Problèmes à corriger\" section of the directives, before any other consideration.\n"
                  + "Every problem must be addressed in this new text.\n";

        // 2. Contrainte de longueur (suit le rôle de base) — qualité toujours exigée, seul le seuil chiffré varie
        String lengthConstraint = "Develop the scene with sensory depth and precision. "
                + (in.minWords() > 0
                    ? "This sequence must be at least " + in.minWords() + " words long."
                    : "This sequence is between 300 and 800 words long.")
                + " If you reach the final beat before that length, deepen the beats already written "
                + "(sensations, gestures, dialogue) — never invent new plot events to fill space.";

        // 3. Règle d'ouverture : stitch (consigne explicite de transition) > continuité par défaut > point d'entrée varié
        boolean hasStitch = in.stitch() != null && !in.stitch().isBlank();
        boolean hasPrev   = in.previousSequenceText() != null && !in.previousSequenceText().isBlank();
        String openingRule = hasStitch
                ? "Apply this instruction to handle the transition from the previous sequence to this one: "
                  + in.stitch()
                : hasPrev
                        ? "Continue directly from where the previous sequence's text stops — "
                          + "those sentences are already written, your first sentence is the next one in the story: carry the action forward. "
                          + "Do not restate the setting or the general atmosphere already established — "
                          + "unless the directives explicitly ask for it."
                        : "Vary the entry point of your first sentence: a physical action, a fragment of dialogue, "
                          + "a sensory detail, an inner thought, an isolated object or gesture. "
                          + "Never open on a bare character name or a bare subject pronoun "
                          + "('He', 'She', 'They') — show something happening first; the character is its implicit subject.";

        return rewritePrefix
                + """
                You are a literary fiction writer.
                Follow the author's detailed directives for this sequence, in the order given.
                Every beat they list MUST appear in the text, in that same order — expand each beat into
                prose; never copy a beat sentence as-is and never keep its number. Dialogue quoted in a
                beat may be kept close to as written — the ban targets narrative/description beats, not
                spoken lines.
                You may enrich the scene with local actions, reactions, observations or dialogue, as long
                as they do not alter the required elements, the relationships between characters, or the
                outcome of the scene.
                Output only the narrative text: no title, no headings, no beat numbers, no notes, no
                meta-commentary before or after the prose.
                If instructions conflict, apply this priority order:
                1. Author's detailed directives
                2. Writing requirements
                3. Character sheets
                4. Style guide
                5. Writing example
                6. Focus
                7. Lore
                Forbidden phrases, narrative patterns to avoid, already-established entity states and the
                previous sequence's text are never part of this arbitration: they are absolute facts or
                absolute bans, respected no matter what.
                """
                + lengthConstraint + "\n" + openingRule + "\n"
                + """
                Write only the events of this sequence — never anticipate later sequences.
                If a character sheet specifies an article and pronoun (line 'Article : X — pronoun'),
                apply them strictly throughout. A character's visible traits (clothing, physical
                appearance, recurring gestures) and temperament are non-negotiable facts — never quote or
                paraphrase the sheet; embody the traits through concrete behavior. Example: for "nervous,
                bites her nails", write her tapping her fingers on the table, not "she was nervous".
                The sensory notes are ingredients, not a checklist: weave some of them naturally into the
                prose; do not enumerate them in one block.
                The text must not read as machine-written: prefer everyday vocabulary and natural
                phrasing, avoid wording that sounds artificial. This naturalness rule yields only to an
                explicit contrary instruction in the style guide.
                Write the prose in French.
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
