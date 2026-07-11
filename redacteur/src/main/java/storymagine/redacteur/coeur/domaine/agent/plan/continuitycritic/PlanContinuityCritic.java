package storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.LanguageNames;
import storymagine.commun.coeur.domaine.text.SummaryBudget;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Axis D of the chapter-plan critic suite: does the plan continue the earlier chapters already
 * planned for this book — ongoing arcs, overall tone, threads the reader expects to see again?
 * No fact-checking here (that's PlanFactsCritic's job): only narrative continuity. Called only
 * when at least one earlier chapter's plan exists — the book's first chapter has nothing to
 * compare against (see PlanContinuityCriticStep).
 */
public class PlanContinuityCritic implements Agent {

    private static final String AGENT_NAME = "PlanContinuityCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanContinuityCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanContinuityCriticOutput call(PlanContinuityCriticInput input) {
        String system = buildSystem(input.language());

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Chapter goal",           t.text(input.chapterGoal(), 800, "chapterGoal"))
                .section("Chapter description",    t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
                .section("Earlier chapters' plans", t.tailText(input.previousChaptersPlans(), SummaryBudget.charBudget(ctx), "previousChaptersPlans"))
                .section("Plan",                   t.text(input.plan(), ctx * 4 * 40 / 100, "plan"))
                .raw("Check the plan's continuity with the earlier chapters' plans, then list your findings.")
                .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanContinuityCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String buildSystem(String language) {
        return """
            You are PlanContinuityCritic. Your single mission: verify that the plan continues
            the earlier chapters already planned for this book — ongoing arcs, overall tone,
            threads the reader expects to see again.

            RULE OF PRIMACY
            The chapter goal and description always prevail. A change of arc, tone or
            direction explicitly asked by them is never a defect. Flag a break only when
            the plan introduces it on its own.

            INPUT
            - Chapter goal and chapter description: the author's instruction (for the rule above).
            - Earlier chapters' plans: what previous chapters have already set up.
            - Plan: the generated plan (sequences and beats) for the current chapter.

            WHAT TO CHECK
            From the earlier chapters' plans, identify the ongoing arcs, the dominant tone
            and the open threads, then check how the current plan picks them up.
            - DEFAUT_MAJEUR: the plan reverses or abandons the central ongoing arc,
              and the instruction does not ask for it.
            - DEFAUT_SIGNIFICATIF: the plan ignores an open thread an earlier chapter
              presents as pressing, or breaks the story's tone with nothing to motivate it.
            - AMELIORATION: the plan misses a natural callback to an earlier chapter
              that would strengthen continuity.

            OUT OF SCOPE
            Do not check individual facts (wounds, objects, dates — another critic does),
            fidelity to the instruction, the plan's internal consistency, or dramatic quality.

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
            Earlier chapters' plans: three chapters where the crew hunts a traitor hidden
            among them; suspicion poisons every scene.
            Plan: a quiet market day; the traitor is never mentioned. The goal and the
            description do not ask for a pause in that thread.
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            - The hunt for the traitor, central for three chapters, is absent from the plan, and the instruction does not ask for a pause in that thread.
            DEFAUT_MAJEUR:
            [RIEN]
            """
            + "\nWrite your findings in " + LanguageNames.english(language) + ".\n";
    }
}
