package storymagine.chat.coeur.domaine.agent.nextactreadiness;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatTurn;

import java.util.List;

/**
 * Assembles the system + user text sent to the LLM for a NextActReadinessAnalyst call — same
 * shape as ChatPromptBuilder (fixed system framing, variable user context in the order it appears
 * in the final prompt) but with its own system prompt : this is an out-of-character analysis, not
 * a roleplay turn, so it does not reuse ChatPromptBuilder's SYSTEM_INTRO/SYSTEM_FORMATTING.
 */
public final class NextActReadinessPromptBuilder {

    private NextActReadinessPromptBuilder() {}

    public record NextActPrompt(String system, String user) {}

    private static final String SYSTEM = """
        You are analyzing this roleplay session as its author — not as any character in it. Do not
        write narration, dialogue, or actions. Your only job is to judge whether the story is ready
        to move to the next act.

        OUTPUT FORMAT
        Write your answer in the same language as the scenario text below. Write exactly these
        three sections, nothing before or after:
        CONDITION:
        STATE:
        MISSING:
        - CONDITION: restate, in one or two sentences, what needs to happen for the story to move
          to the next act — in your own words, not copied verbatim from the act text.
        - STATE: has it fully happened, partly happened, or not at all, based only on what has
          actually occurred in the story so far (not what feels likely to happen soon)?
        - MISSING: what concretely still needs to happen. If the condition is already fully met,
          write the single line [RIEN].

        EXAMPLE (in English; your own output stays in the scenario's language)
        Act text: "...they explore the cellar. [NEXT ACT] once they find the hidden door."
        CONDITION:
        The story moves on once the characters find the hidden door in the cellar.
        STATE:
        Not met yet — they are still searching the cellar, no door has been found or even mentioned.
        MISSING:
        They still need to actually discover the hidden door somewhere in the cellar.""";

    /** currentAct must be a valid 1-based act with a next act pending — callers check this first (see ChatServiceImpl). */
    public static NextActPrompt build(ChatScenario scenario, int currentAct, String summary, List<ChatTurn> recentTurns) {
        ScenarioAct act = scenario.acts().get(currentAct - 1);

        StringBuilder user = new StringBuilder();
        user.append("CHARACTER SHEET:\n").append(scenario.characterSheet()).append("\n\n");
        user.append("PREMISE:\n").append(scenario.premise()).append("\n\n");
        user.append("CURRENT ACT (").append(currentAct).append(" of ").append(scenario.acts().size())
            .append("):\n").append(act.text()).append("\n\n");
        if (summary != null && !summary.isBlank()) {
            user.append("STORY SO FAR:\n").append(summary).append("\n\n");
        }
        String characterLabel = ChatPromptBuilder.characterLabel(scenario);
        if (!recentTurns.isEmpty()) {
            user.append("Recent exchange:\n").append(ChatPromptBuilder.transcript(recentTurns, characterLabel));
        }

        return new NextActPrompt(SYSTEM, user.toString());
    }
}
