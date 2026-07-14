package storymagine.chat.coeur.domaine.agent.continuityreviewer;

import storymagine.chat.coeur.domaine.agent.commun.ReviewOutputParser;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;

/**
 * Reads a scenario's acts in order, one at a time, keeping everything already read as "story so
 * far", and reports narrative inconsistencies against that accumulated context. See
 * ScenarioContinuityReviewer.md.
 */
public class ScenarioContinuityReviewer {

    private static final String AGENT_NAME  = "ScenarioContinuityReviewer";
    private static final double TEMPERATURE = 0.3;

    private final ModelCallPort llm;

    public ScenarioContinuityReviewer(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public ScenarioContinuityReviewerOutput call(ScenarioContinuityReviewerInput input) {
        String user = "CHARACTER SHEET:\n" + input.characterSheet()
            + "\n\nPREMISE:\n" + input.premise()
            + "\n\nSTORY SO FAR:\n" + emptyIfBlank(input.storySoFar())
            + "\n\nCURRENT ACT (" + input.actNumber().display() + " " + input.actTitle() + "):\n" + input.actText();

        String raw = llm.generate(SYSTEM, user, TEMPERATURE,
            LlmCallContext.of(agentName(), input.actNumber().display())).text();
        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);
        return new ScenarioContinuityReviewerOutput(review.issues(), review.suggestions());
    }

    private static String emptyIfBlank(String s) {
        return (s == null || s.isBlank()) ? "(empty — this is the first act)" : s;
    }

    private static final String SYSTEM = """
        You are ScenarioContinuityReviewer. You check whether the current act of a roleplay
        scenario is consistent with everything established before it.

        INPUT
        - Character sheet: who the roleplay LLM plays.
        - Premise: the fixed backdrop of the story.
        - Story so far: the acts already reviewed, numbered, in order.
        - Current act: the act to review now.

        WHAT TO CHECK
        Compare the current act against the character sheet, the premise, and the story so far.
        Report only:
        - The act treats a character, object or fact as already known, but nothing above
          introduced it. (An element openly introduced as NEW in this act is normal.)
        - The act contradicts an established fact (place, relationship, event, timeline).
        - A plan or promise from an earlier act is dropped with no explanation.
        An unresolved mystery is not an issue: it may be resolved in a later act.
        Do not judge style or pacing — only consistency.
        Each issue must name the act it conflicts with (e.g. "act 2 says...").

        OUTPUT FORMAT
        Write your findings in the same language as the scenario text above. Write exactly these
        two sections, nothing before or after:
        ISSUES:
        SUGGESTIONS:
        One line per item, starting with "- ". ISSUES = inconsistencies only. SUGGESTIONS =
        improvements (a missing link to add, a fact to introduce earlier). If a section is empty,
        write the single line [RIEN] under it.

        EXAMPLES (in English; your own output stays in the scenario's language)
        1) Story so far (act 1): "Mara has never left her village."
           Current act: "Mara recalls the towers of the capital, where she grew up."
        ISSUES:
        - Act 1 says Mara has never left her village, but this act says she grew up in the capital.
        SUGGESTIONS:
        - Make the capital a story Mara heard from a traveler instead of a memory.
        2) Nothing to report:
        ISSUES:
        [RIEN]
        SUGGESTIONS:
        [RIEN]
        """;
}
