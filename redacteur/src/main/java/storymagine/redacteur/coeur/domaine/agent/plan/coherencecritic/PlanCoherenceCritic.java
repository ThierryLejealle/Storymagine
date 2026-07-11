package storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.LanguageNames;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Axis C of the chapter-plan critic suite: does the plan introduce, on its own, a
 * contradiction or an impossible chain of events between its own sequences? Purely internal —
 * no fiches, no established facts, no consigne beyond the sequence directives (for the
 * primacy rule). Established facts and consigne fidelity are other critics' job.
 */
public class PlanCoherenceCritic implements Agent {

    private static final String AGENT_NAME = "PlanCoherenceCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanCoherenceCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanCoherenceCriticOutput call(PlanCoherenceCriticInput input) {
        String system = buildSystem(input.language());

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Sequence directives", t.text(input.sequenceDirectives(), ctx * 4 / 8, "sequenceDirectives"))
                .section("Plan",                t.text(input.plan(), ctx * 4 * 55 / 100, "plan"))
                .raw("Check the plan's internal coherence, then list your findings.")
                .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanCoherenceCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String buildSystem(String language) {
        return """
            You are PlanCoherenceCritic. Your single mission: find contradictions or impossible
            chains of events that the plan creates, on its own, between its own sequences.

            RULE OF PRIMACY
            The sequence directives always prevail. A rupture, a sudden change of state or an
            abrupt shift explicitly asked by a directive is never a defect. Flag a break only
            when the plan introduces it without any directive asking for it.

            INPUT
            - Sequence directives: what the author asks for each sequence (for the rule above).
            - Plan: the generated plan (sequences and beats).

            WHAT TO CHECK
            Read the sequences in order and verify that each one can follow from the previous
            ones: characters in coherent places and states, causes before effects, no event
            undone without explanation.
            - DEFAUT_MAJEUR: a sequence is impossible given a previous one (a character acts
              after dying, an effect comes before its cause, a destroyed place is intact again).
            - DEFAUT_SIGNIFICATIF: a sequence contradicts a previous one on a state
              (an object, an injury, knowledge a character cannot have yet).
            - AMELIORATION: a transition between sequences is abrupt or unexplained,
              without being contradictory.

            OUT OF SCOPE
            Do not judge fidelity to the chapter goal, facts established before this chapter,
            or dramatic quality.

            OUTPUT FORMAT
            Write exactly these three sections, in this order, and nothing else —
            no score, no comment, no introduction:
            AMELIORATION:
            DEFAUT_SIGNIFICATIF:
            DEFAUT_MAJEUR:
            Under each header, one line per problem, starting with "- " and naming the
            sequences concerned. If a section has no problem, write the single line [RIEN].

            EXAMPLE
            Plan, sequence 2: "Les villageois fuient le village en flammes."
            Plan, sequence 4, la même nuit : "Les villageois dînent tranquillement sur la place du village."
            No directive asks for their return.
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            [RIEN]
            DEFAUT_MAJEUR:
            - Sequence 4: les villageois dînent sur la place la même nuit où ils ont fui le village en flammes en séquence 2, et rien n'explique leur retour.
            """
            + "\nWrite your findings in " + LanguageNames.english(language) + ".\n";
    }
}
