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

import java.util.List;

/**
 * Axis D of the chapter-plan critic suite: does the plan continue the story already written —
 * ongoing arcs, overall tone, threads the reader expects to see again? No fact-checking here
 * (that's PlanFactsCritic's job): only narrative continuity with the running story summary.
 * Empty summary (book's first chapter) means nothing to check against.
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
        if (input.summary() == null || input.summary().isBlank()) {
            return new PlanContinuityCriticOutput(List.of(), 10.0);
        }

        String system = buildSystem(input.language());

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Chapter goal",        t.text(input.chapterGoal(), 800, "chapterGoal"))
                .section("Chapter description", t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
                .section("Story summary",       t.tailText(input.summary(), SummaryBudget.charBudget(ctx), "summary"))
                .section("Plan",                t.text(input.plan(), ctx * 4 * 40 / 100, "plan"))
                .raw("Check the plan's continuity with the story summary, then list your findings.")
                .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanContinuityCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String buildSystem(String language) {
        return """
            You are PlanContinuityCritic. Your single mission: verify that the plan continues
            the story already written — ongoing arcs, overall tone, threads the reader
            expects to see again.

            RULE OF PRIMACY
            The chapter goal and description always prevail. A change of arc, tone or
            direction explicitly asked by them is never a defect. Flag a break only when
            the plan introduces it on its own.

            INPUT
            - Chapter goal and chapter description: the author's instruction (for the rule above).
            - Story summary: what the previous chapters have established.
            - Plan: the generated plan (sequences and beats).

            WHAT TO CHECK
            From the summary, identify the ongoing arcs, the dominant tone and the open
            threads, then check how the plan picks them up.
            - DEFAUT_MAJEUR: the plan reverses or abandons the central ongoing arc,
              and the instruction does not ask for it.
            - DEFAUT_SIGNIFICATIF: the plan ignores an open thread the summary presents
              as pressing, or breaks the story's tone with nothing to motivate it.
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
            Story summary: "Depuis trois chapitres, l'équipage traque le traître caché parmi eux ;
            la suspicion empoisonne chaque scène."
            Plan: une journée de marché tranquille ; le traître n'est jamais mentionné. Le but et
            la description ne demandent pas de pause dans cette intrigue.
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            - La traque du traître, centrale depuis trois chapitres, est absente du plan, et la consigne ne demande pas de pause dans cette intrigue.
            DEFAUT_MAJEUR:
            [RIEN]
            """
            + "\nWrite your findings in " + LanguageNames.english(language) + ".\n";
    }
}
