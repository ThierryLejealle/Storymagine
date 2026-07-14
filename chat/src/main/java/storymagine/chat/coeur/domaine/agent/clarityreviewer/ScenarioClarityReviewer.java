package storymagine.chat.coeur.domaine.agent.clarityreviewer;

import storymagine.chat.coeur.domaine.agent.commun.ReviewOutputParser;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;

/**
 * Reads ONE act in isolation, exactly as the real RoleplayNarrator would see it (character sheet +
 * premise + that act's resolved text, nothing before or after), and judges whether it is usable as
 * a prompt as-is — including whether the "[NEXT ACT]" trigger condition is understandable. See
 * ScenarioClarityReviewer.md.
 */
public class ScenarioClarityReviewer {

    private static final String AGENT_NAME  = "ScenarioClarityReviewer";
    private static final double TEMPERATURE = 0.3;

    private final ModelCallPort llm;

    public ScenarioClarityReviewer(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public ScenarioClarityReviewerOutput call(ScenarioClarityReviewerInput input) {
        String user = "CHARACTER SHEET:\n" + input.characterSheet()
            + "\n\nPREMISE:\n" + input.premise()
            + "\n\nCURRENT ACT (" + input.actNumber().display() + " " + input.actTitle() + "):\n" + input.actText();

        String raw = llm.generate(SYSTEM, user, TEMPERATURE,
            LlmCallContext.of(agentName(), input.actNumber().display())).text();
        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);
        return new ScenarioClarityReviewerOutput(review.issues(), review.suggestions());
    }

    private static final String SYSTEM = """
        You are ScenarioClarityReviewer. You read ONE act of a roleplay scenario exactly as the
        roleplay model itself would see it — no earlier acts, no later ones — and judge whether it
        is usable as-is.

        INPUT
        - Character sheet: who the roleplay LLM plays.
        - Premise: the fixed backdrop of the story.
        - Current act: the only act you can see.

        WHAT TO CHECK
        - Is the situation and the character's role in it clear from this text alone?
        - Is there anything ambiguous, contradictory, or missing that a model would need to guess?
        - The [NEXT ACT] trigger: try to restate its condition as ONE concrete, observable event
          in the conversation (something said or done). If you cannot, or if the condition is
          vague, contradictory, or missing entirely, report it.
        Do not judge whether the act is consistent with a larger story — you cannot see the rest
        of it, that is another reviewer's job.

        OUTPUT FORMAT
        Write your findings in the same language as the scenario text above. Write exactly these
        two sections, nothing before or after:
        ISSUES:
        SUGGESTIONS:
        One line per item, starting with "- ". If a section is empty, write the single line
        [RIEN] under it.

        EXAMPLES (in English; your own output stays in the scenario's language)
        1) Current act: "...they explore the cellar. [NEXT ACT] when it feels right."
        ISSUES:
        - The condition to write [NEXT ACT] ("when it feels right") gives no concrete signal to
          act on.
        SUGGESTIONS:
        - Replace it with a concrete trigger, e.g. "when they find the hidden door."
        2) Nothing to report:
        ISSUES:
        [RIEN]
        SUGGESTIONS:
        [RIEN]
        """;
}
