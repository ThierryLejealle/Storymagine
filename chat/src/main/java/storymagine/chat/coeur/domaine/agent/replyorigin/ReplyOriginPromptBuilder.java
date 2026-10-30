package storymagine.chat.coeur.domaine.agent.replyorigin;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ReplyTrace;

/**
 * Assembles the system + user text sent to the LLM for a ReplyOriginAnalyst call. The system part
 * frames a neutral, purely descriptive mapping of the reply onto the prompt (see
 * ReplyOriginAnalyst.md) ; the user part carries the analysed prompt, reply and reasoning between
 * unmistakable separators, because the analysed prompt has headings of its own.
 */
public final class ReplyOriginPromptBuilder {

    private ReplyOriginPromptBuilder() {}

    public record OriginPrompt(String system, String user) {}

    /** Replaced in SYSTEM by the scenario's language (see ScenarioLanguage.phrase). */
    private static final String LANGUAGE_PLACEHOLDER = "{LANGUAGE}";

    private static final String SYSTEM = """
        You are a neutral observer who studies a roleplay exchange from the outside. You are not any
        character in it. You receive the PROMPT a character was given and the REPLY it wrote, and,
        when available, the REASONING it produced before replying. Your job is to map the reply onto
        the prompt, like a traceability table: for each element of the reply, find the passage of the
        prompt that relates to it. Describe correspondences only; nothing is evaluated. You cannot
        know why the reply was written, so every finding is a lead for the reader to check.

        HOW TO WORK
        1. Split the reply into elements: each action, each fact stated about the world or the
           characters, each idea expressed in dialogue, and the form of the reply (language, length,
           formatting, tone).
        2. For each element, look through the whole prompt for a passage that states it, implies it
           or sets a rule for it. The prompt is made of labelled blocks: the opening game rules, YOUR
           CHARACTER, ALSO PRESENT, NOT IN THE SCENE, SCENARIO, CURRENT SCENE, STORY SO FAR, STYLE
           ADVICE, FORMATTING with the rules that follow it, and the recent exchange (the user part).
        3. When REASONING is given, use it as a pointer: the passages it mentions or paraphrases are
           strong candidates.

        OUTPUT FORMAT
        Write your answer in {LANGUAGE}, except the quotes, which stay exactly as in the prompt.
        Write exactly these three sections, nothing before or after:
        TRACED:
        UNTRACED:
        READING:
        - TRACED: one line per element that has a matching passage, written
          "- <element> -> <block> : "<short exact quote>" [direct]" when the passage states or
          commands the element, or [indirect] when it only suggests it or is close in theme.
          Write [RIEN] if there is no such element.
        - UNTRACED: one line per element for which no passage exists, written "- <element>".
          Write [RIEN] if there is none.
        - READING: two or three sentences naming the blocks that appear most among the matches, and
          what the untraced elements have in common if there are any.

        EXAMPLE (in English; your own output stays in the language asked above)
        The prompt holds CURRENT SCENE: "The party must go down into the cellar." and STYLE ADVICE:
        "Short sentences." The reply is:
        *I pull a lantern from under the counter.* "The cellar door is behind the barrels."
        TRACED:
        - the cellar door -> CURRENT SCENE : "go down into the cellar" [direct]
        - short sentences -> STYLE ADVICE : "Short sentences." [direct]
        - the words come as an answer to the player -> the recent exchange : "Where is the way down?" [indirect]
        UNTRACED:
        - the lantern under the counter
        READING:
        The scene text and the style advice shape the dialogue and its form. The lantern has no
        counterpart in the prompt.""";

    public static OriginPrompt build(ChatScenario scenario, ReplyTrace trace) {
        StringBuilder user = new StringBuilder();
        user.append("===== PROMPT, SYSTEM PART =====\n").append(trace.system()).append("\n\n");
        user.append("===== PROMPT, USER PART =====\n").append(trace.user()).append("\n\n");
        user.append("===== REPLY =====\n").append(trace.reply()).append('\n');
        if (!trace.thinking().isBlank()) {
            user.append("\n===== REASONING BEFORE THE REPLY =====\n").append(trace.thinking()).append('\n');
        }
        return new OriginPrompt(
            SYSTEM.replace(LANGUAGE_PLACEHOLDER, scenario.language().phrase("the same language as the prompt below")),
            user.toString());
    }
}
