package storymagine.redacteur.coeur.domaine.agent.plan.factscritic;

import storymagine.commun.coeur.domaine.prompt.PromptBuilder;
import storymagine.commun.coeur.domaine.text.LanguageNames;
import storymagine.commun.coeur.domaine.text.TruncHelper;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.redacteur.coeur.domaine.agent.Agent;
import storymagine.redacteur.coeur.domaine.agent.commun.CriticOutputParser;

/**
 * Axis B of the chapter-plan critic suite: does the plan invent, without the instruction
 * asking for it, a contradiction with an already-established fact (character sheets, checks,
 * entity state)? The author's instruction always prevails — a change it explicitly asks for
 * is never a defect, even if it contradicts a sheet or a fact established elsewhere.
 */
public class PlanFactsCritic implements Agent {

    private static final String AGENT_NAME = "PlanFactsCritic";

    private final ModelCallPort llm;
    private final LogPort       log;

    @Override
    public String agentName() { return AGENT_NAME; }

    public PlanFactsCritic(ModelCallPort llm, LogPort log) {
        this.llm = llm;
        this.log = log;
    }

    public PlanFactsCriticOutput call(PlanFactsCriticInput input) {
        String system = buildSystem(input.language());

        int ctx = llm.contextWindow();
        TruncHelper t = TruncHelper.create();
        String user = PromptBuilder.create()
                .section("Chapter goal",        t.text(input.chapterGoal(), 800, "chapterGoal"))
                .section("Chapter description", t.text(input.chapterDescription(), ctx * 4 / 12, "chapterDescription"))
                .section("Character sheets",    t.blockList(input.characters(), ctx * 4 / 8, "characters"))
                .section("Checks",              t.list(input.checks(), ctx * 4 / 8, "checks"))
                .section("Entity state",        t.list(input.entityState(), ctx * 4 / 12, "entityState"))
                .section("Plan",                t.text(input.plan(), ctx * 4 * 55 / 100, "plan"))
                .raw("Check the plan against the sheets, the checks and the entity state, then list your findings.")
                .build();
        t.logIfTruncated(log, agentName());

        String raw = llm.generate(system, user, 0.3, LlmCallContext.of(agentName(), agentLabel()).withThink(thinks())).text();
        return new PlanFactsCriticOutput(CriticOutputParser.parseProblems(raw), CriticOutputParser.calculateScore(raw));
    }

    private static String buildSystem(String language) {
        return """
            You are PlanFactsCritic. Your single mission: find contradictions that the plan
            invents, on its own, against facts already established in the story.

            RULE OF PRIMACY
            The chapter goal and description always prevail. Anything they explicitly ask for
            is never a defect, even if it contradicts an established fact. Flag a contradiction
            only when the plan introduces it without the goal or description asking for it.

            INPUT
            - Chapter goal and chapter description: the author's instruction (for the rule above).
            - Character sheets: established facts about each character.
            - Checks: facts the author declared must always hold.
            - Entity state: current state of places, objects and relationships.
            - Plan: the generated plan (sequences and beats).

            WHAT TO CHECK
            Compare each beat of the plan with the sheets, the checks and the entity state.
            - DEFAUT_MAJEUR: the plan contradicts a check, or a central established fact
              (a death, a lost limb, a revealed identity).
            - DEFAUT_SIGNIFICATIF: the plan contradicts a secondary established fact
              (a skill, a location, a relationship).
            - AMELIORATION: a beat strains against an established fact without contradicting
              it (e.g. a shy character giving a speech with no transition). You must be able
              to name the sheet, check or entity-state fact concerned. If you cannot, it is
              staging, not a defect: do not report it.

            OUT OF SCOPE
            Do not judge fidelity to the instruction, the plan's internal consistency,
            narrative continuity with previous chapters, or dramatic quality.
            Never judge staging or plausibility: who is present or absent in a sequence,
            unexplained arrivals or departures, fast transitions. A character appearing
            without a companion is not a contradiction unless an established fact requires
            them to be together.
            Checks may also restrict what you are allowed to report: obey them.

            OUTPUT FORMAT
            Write exactly these three sections, in this order, and nothing else —
            no score, no comment, no introduction:
            AMELIORATION:
            DEFAUT_SIGNIFICATIF:
            DEFAUT_MAJEUR:
            Under each header, one line per problem, starting with "- " and naming the
            sequence concerned. If a section has no problem, write the single line [RIEN].

            EXAMPLE
            Character sheet: "Joran lost his left arm in chapter 3."
            Plan, sequence 1: "Joran climbs the cliff, gripping the rock with both hands."
            Nothing in the goal or description asks for this.
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            [RIEN]
            DEFAUT_MAJEUR:
            - Sequence 1: Joran climbs with both hands, but his sheet says he lost his left arm in chapter 3, and the instruction does not ask for this.

            COUNTER-EXAMPLE (must NOT be reported)
            Plan, sequence 1: "Elena and Marco arrive together at the festival."
            Plan, sequence 2: "Elena, alone, signs up at the booth."
            No sheet, check or entity state says they must stay together: this is staging.
            Expected output:
            AMELIORATION:
            [RIEN]
            DEFAUT_SIGNIFICATIF:
            [RIEN]
            DEFAUT_MAJEUR:
            [RIEN]
            """
            + "\nWrite your findings in " + LanguageNames.english(language) + ".\n";
    }
}
