package storymagine.redacteur.coeur.domaine.agent.plan.goalcritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.LanguageNames;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Axis A of the chapter-plan critic suite: does the plan deliver, beat by beat, every
 * explicit element of the author's instruction (chapter goal, description, and each
 * sequence's directive)? This IS the comparison to the instruction — no primacy clause
 * needed, unlike the other plan critics.
 */
public class PlanGoalCritic implements Agent {

    private static final String AGENT_NAME = "PlanGoalCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanGoalCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanGoalCriticOutput call(PlanGoalCriticInput input) {
        String system = buildSystem(input.language());

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Chapter goal",         t.text(input.chapterGoal(), 1600, "chapterGoal"))
                .section("Chapter description",  t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
                .section("Sequence directives",  t.text(input.sequenceDirectives(), ctx * 4 / 8, "sequenceDirectives"))
                .section("Book goal (context)",  t.text(input.bookGoal(), 1600, "bookGoal"))
                .section("Plan",                 t.text(input.plan(), ctx * 4 / 2, "plan"))
                .raw("Check the plan against the goal, the description and each sequence directive, then list your findings.")
                .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanGoalCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String buildSystem(String language) {
        return """
            You are PlanGoalCritic. Your single mission: verify that the chapter plan delivers,
            beat by beat, every explicit element of the author's instruction.

            INPUT
            - Chapter goal: what the chapter must achieve.
            - Chapter description: how the author describes the chapter.
            - Sequence directives: what each sequence must contain.
            - Plan: the generated plan (sequences and beats).

            WHAT TO CHECK
            Go through the goal, the description and each directive, element by element,
            and find where the plan delivers each one.
            - DEFAUT_MAJEUR: an explicit element is missing from the plan, or the plan states its opposite.
            - DEFAUT_SIGNIFICATIF: an element is present but distorted — wrong sequence,
              weakened to the point of changing its meaning, or delivered only by implication.
            - AMELIORATION: an element is delivered but vaguely; the plan could state it more directly.

            OUT OF SCOPE
            Do not judge the plan's internal consistency, established story facts,
            or dramatic quality — other critics handle those. What the plan adds beyond
            the instruction is not your concern either: you only track what the instruction
            asks and the plan fails to deliver.

            OUTPUT FORMAT
            Write exactly these three sections, in this order, and nothing else —
            no score, no comment, no introduction:
            AMELIORATION:
            DEFAUT_SIGNIFICATIF:
            DEFAUT_MAJEUR:
            Under each header, one line per problem, starting with "- " and naming the
            sequence concerned. If a section has no problem, write the single line [RIEN].

            EXAMPLE
            Sequence 2 directive: "Mara refuses the smugglers' offer."
            Plan, sequence 2: "Mara hesitates, then accepts the smugglers' offer."
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            [RIEN]
            DEFAUT_MAJEUR:
            - Sequence 2: the directive says Mara refuses the offer, the plan makes her accept it.
            """
            + "\nWrite your findings in " + LanguageNames.english(language) + ".\n";
    }
}
