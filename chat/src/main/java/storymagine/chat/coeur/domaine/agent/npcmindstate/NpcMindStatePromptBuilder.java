package storymagine.chat.coeur.domaine.agent.npcmindstate;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder;
import storymagine.chat.coeur.domaine.session.ChatTurn;

import java.util.List;

/**
 * Assembles the system + user text sent to the LLM for a NpcMindStateAnalyst call — same shape as
 * ChatPromptBuilder (fixed system framing, variable user context in the order it appears in the
 * final prompt) but with its own system prompt : this is an out-of-character analysis, not a
 * roleplay turn, so it does not reuse ChatPromptBuilder's SYSTEM_INTRO/SYSTEM_FORMATTING.
 */
public final class NpcMindStatePromptBuilder {

    private NpcMindStatePromptBuilder() {}

    public record MindStatePrompt(String system, String user) {}

    private static final String SYSTEM = """
        You are analyzing this roleplay session as its author — not as the character itself. Do
        not write narration, dialogue, or actions. Your only job is to describe the character's
        current state of mind, honestly and privately — this is never shown to the character,
        only to the person running the story.

        OUTPUT FORMAT
        Write your answer in the same language as the scenario text below. Write exactly these
        three sections, nothing before or after:
        SITUATION:
        THOUGHTS:
        PLANS:
        - SITUATION: in one or two sentences, what is actually happening around the character
          right now, from their point of view.
        - THOUGHTS: what the character privately thinks and feels about it — not what they'd say
          out loud, their honest internal reaction.
        - PLANS: what the character currently intends to do next, if anything concrete. If they
          have no particular plan right now, write the single line [RIEN].

        EXAMPLE (in English; your own output stays in the scenario's language)
        SITUATION:
        The player has just accused her of lying about the letter, and is waiting for a response.
        THOUGHTS:
        She is furious he saw through her so quickly — but also, quietly, relieved to be caught.
        PLANS:
        Deny it once more, then if pressed again, admit part of the truth to win back his trust.""";

    /** currentAct is 0 if the scenario has no acts (or none is active) — same convention as ChatPromptBuilder. */
    public static MindStatePrompt build(ChatScenario scenario, Npc speaker, int currentAct, String summary,
                                         List<ChatTurn> recentTurns) {
        StringBuilder user = new StringBuilder();
        user.append("CHARACTER SHEET:\n").append(speaker.fullSheet()).append("\n\n");
        user.append("PREMISE:\n").append(scenario.premise()).append("\n\n");
        List<ScenarioAct> acts = scenario.acts();
        if (currentAct > 0 && currentAct <= acts.size()) {
            ScenarioAct act = acts.get(currentAct - 1);
            user.append("CURRENT ACT (").append(currentAct).append(" of ").append(acts.size())
                .append("):\n").append(act.text()).append("\n\n");
        }
        if (summary != null && !summary.isBlank()) {
            user.append("STORY SO FAR:\n").append(summary).append("\n\n");
        }
        if (!recentTurns.isEmpty()) {
            user.append("Recent exchange:\n")
                .append(ChatPromptBuilder.transcript(recentTurns, scenario.cast(), scenario.playerName()));
        }

        return new MindStatePrompt(SYSTEM, user.toString());
    }
}
