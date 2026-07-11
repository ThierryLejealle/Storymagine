package storymagine.redacteur.coeur.domaine.agent.plan.dramacritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.LanguageNames;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Axis E of the chapter-plan critic suite: does the plan make the chapter alive and
 * interesting at the intensity the author's instruction itself calls for? Judgment is always
 * relative to the intended intensity (a calm chapter asked to be calm is never flat) — never
 * an absolute tension bar. Suggested by Fable during the plan-critic redesign consultation.
 */
public class PlanDramaCritic implements Agent {

    private static final String AGENT_NAME = "PlanDramaCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanDramaCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanDramaCriticOutput call(PlanDramaCriticInput input) {
        String system = buildSystem(input.language());

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Chapter goal",        t.text(input.chapterGoal(), 800, "chapterGoal"))
                .section("Chapter description", t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
                .section("Plan",                t.text(input.plan(), ctx * 4 / 2, "plan"))
                .raw("Judge the plan's dramaturgical effort at the intensity the instruction calls for, then list your findings.")
                .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanDramaCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String buildSystem(String language) {
        return """
            You are PlanDramaCritic. Your single mission: judge whether the plan makes the
            chapter alive and interesting at the intensity the author's instruction calls for.

            RULE OF PRIMACY
            The intended intensity is set by the goal and description, never by you.
            A calm chapter the author asked to be calm is never flat. Judge every sequence
            against the intensity the instruction announces, not against an absolute
            level of tension.

            INPUT
            - Chapter goal and chapter description: the author's instruction,
              including its intended intensity.
            - Plan: the generated plan (sequences and beats).

            WHAT TO CHECK
            - DEFAUT_MAJEUR: the plan delivers far below the intensity the instruction
              announces (the description promises a confrontation or a revelation,
              and the beats stay polite, static or evasive).
            - DEFAUT_SIGNIFICATIF: a sequence is empty of incident — its beats only
              restate the directive or describe a setting while nothing happens,
              even quietly.
            - AMELIORATION: beats repeat each other, or a concrete detail could make
              a moment more vivid within the intended intensity.

            OUT OF SCOPE
            Do not judge fidelity to the letter of the instruction, the plan's internal
            consistency, or established story facts.

            OUTPUT FORMAT
            Write exactly these three sections, in this order, and nothing else —
            no score, no comment, no introduction:
            AMELIORATION:
            DEFAUT_SIGNIFICATIF:
            DEFAUT_MAJEUR:
            Under each header, one line per problem, starting with "- " and naming the
            sequence concerned when there is one. If a section has no problem, write the
            single line [RIEN].

            EXAMPLE
            Description: "La tension entre les deux frères explose enfin."
            Plan: trois séquences où les frères échangent des propos polis et se quittent
            par une poignée de main.
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            [RIEN]
            DEFAUT_MAJEUR:
            - La description annonce que la tension entre les frères explose enfin, mais chaque beat les garde polis ; l'explosion promise n'a jamais lieu.
            """
            + "\nWrite your findings in " + LanguageNames.english(language) + ".\n";
    }
}
