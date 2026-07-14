package storymagine.chat.coeur.domaine.session;

import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

import java.util.List;

/**
 * Assembles the system + user text sent to the LLM for one turn (ModelCallPort, /api/chat) :
 * system carries the fixed setup (character, scenario, summary, formatting rules), user carries
 * the recent exchange plus the player's new line. Built as fixed/variable parts, in the order
 * they appear in the final prompt, so the shape is readable directly in the code.
 */
public final class ChatPromptBuilder {

    private ChatPromptBuilder() {}

    public record ChatPrompt(String system, String user) {}

    private static final String SYSTEM_INTRO = """
        Let's roleplay. You are the character described below. Stay in character in every reply:
        speak, act and think as they would. Never speak or act for the player's character, unless
        they hand you the narrator's pen with "DO: " (see below). Reply in the same language as
        the character and scenario below.

        Turn scope: when you and the player's character discover something in the world
        together — a room, a corridor, a view — keep narrating it freely, as you already do.
        The one exception: when your action is aimed directly at the player's character —
        you let them in, hand them something, answer them, grant them access — stop right
        after that action. What they see, feel, or do next is theirs to write, not yours.

        Example — a door opens:
        Shared discovery (push open a door onto a dungeon corridor together):
        RIGHT: *The door groans open. Torchlight spills over a chamber lined with broken
        statues, cold air rushing past us.*

        Aimed at the player (you unlock your own door to let them in):
        WRONG: *The lock clicks. I step back and pull the door open. You see a cramped room,
        papers scattered everywhere, and the smell of old smoke hits you.*
        RIGHT: *The lock clicks. I step back and pull the door open, waiting.*

        Length: match each reply to the weight of the moment — never to the size of your
        previous replies.
        - A charged moment (a confession, a shock, a question that truly matters) hits hardest
          in one or two short sentences.
        - A new scene, a discovery, or an action sequence can take one or two full paragraphs.
        - Most replies fall between these two.
        Never pad a short answer to reach a "usual" size.

        Example — Player: "Did you ever love me, or was it all part of the plan?"
        WRONG (padded): *She looks away, her fingers tracing the rim of her cup as a thousand
        memories wash over her, each one heavier than the last.* "That is a complicated
        question, and I am not sure I can give you the answer you deserve. There were moments,
        real moments, but also duties I could not escape..."
        RIGHT (short, heavy moment): *She holds your gaze.* "Every day."

        Example — Player: "I push open the temple doors."
        RIGHT (longer, new scene): *The doors groan against centuries of dust. Cold air rolls
        out, carrying the smell of wax and old stone. Rows of broken statues line the nave,
        their faces chiseled away — someone wanted these gods forgotten.* "Stay close,"
        *she whispers, drawing her blade.* "We are not the first ones here."

        The character sheet below is a private author's note — your character has never
        read it. Show its traits and motives through behavior only; never quote, name or
        explain them. Example, for a character described as shy — Wrong: "Since I'm shy,
        maybe we could stay here." Right: *she hesitates, eyes dropping to the floor*
        "Maybe... we could stay here?\"""";

    private static final String SYSTEM_FORMATTING = """
        FORMATTING — both of us use these conventions:
        - *text between asterisks* is what you DO — a physical action or scene detail.
          Example: *she sets the cup down* "You're late."
        - Plain text with no asterisks is what you SAY.
        - Each of us writes only for our own character, in first person. Never copy the player's
          actions or sensations onto your own character, even ones that sound like they could
          apply to you too — react as an outside observer, from your own character's point of
          view. Example: the player writes *a tear rolls down my cheek*. Wrong: "I feel a tear
          roll down my cheek too." Right: "I watch a tear roll down your cheek."
        - "OOC: ..." means the player is stepping outside the story to talk to you directly — a
          question, an instruction, or a request to change something. Answer as yourself, not as
          your character, and stop there: don't move the scene forward in the same reply. Resume
          the story only once the player sends their next in-character message.
          Example:
          OOC: be more suspicious of me from now on.
          Wrong: "Got it, I'll have him distrust you now." *His eyes narrow and he takes a step
          back, studying my hands.*
          Right: "Got it — I'll play him more guarded going forward."
        - "DO: ..." hands you the narrator's pen for one reply only: advance the scene, narrate
          events, act for other characters if needed. Next turn, return to playing only your
          character.

        Begin and stay in the scene.""";

    /** Fallback used when the current act's text does not itself specify a "[NEXT ACT]" condition. */
    private static final String NEXT_ACT_RULE_DEFAULT = """
        If you feel this act's events have reached their natural conclusion and it's time to move
        the story forward, include "[NEXT ACT]" anywhere in your reply — it will be removed before
        the player sees it, and the story advances to the next act.""";

    /** Used when the current act's text already spells out its own "[NEXT ACT]" condition. */
    private static final String NEXT_ACT_RULE_CONDITIONAL = """
        This act's text above tells you exactly when to write "[NEXT ACT]" — only do it once that
        specific condition has actually happened in the story, never before and never just because
        the scene feels like it could move on. It will be removed before the player sees it, and the
        story advances to the next act.""";

    /**
     * currentAct is 1-based ; 0 means the scenario has no acts (or none is active), in which case
     * no CURRENT ACT section is added — existing act-less scenarios are unaffected.
     */
    public static ChatPrompt build(ChatScenario scenario, int currentAct, String summary,
                                    List<ChatTurn> recentTurns, PlayerMessage input) {
        StringBuilder system = new StringBuilder();
        system.append(SYSTEM_INTRO).append("\n\n");
        system.append("YOUR CHARACTER:\n").append(scenario.characterSheet()).append("\n\n");
        system.append("SCENARIO (this may describe events scripted to happen later, including how you will react\n")
              .append("to them — for your character, that future does not exist yet. You learn of a scripted event\n")
              .append("only when it actually happens in the chat: until then, never hint at it, prepare for it, or\n")
              .append("feel anything about it. Example: if the scenario says you will get angry when the letter\n")
              .append("arrives, you are calm and unsuspecting right up until the letter actually arrives. You are\n")
              .append("free to invent new details and improvise in the present moment — just never reach ahead of\n")
              .append("it):\n")
              .append(scenario.premise()).append("\n\n");
        List<ScenarioAct> acts = scenario.acts();
        if (currentAct > 0 && currentAct <= acts.size()) {
            ScenarioAct act = acts.get(currentAct - 1);
            system.append("CURRENT ACT (").append(currentAct).append(" of ").append(acts.size())
                  .append(") — same anti-spoiler rule as above applies here too:\n")
                  .append(act.text()).append("\n\n");
            if (currentAct < acts.size()) {
                boolean actSpecifiesCondition = act.text().toUpperCase().contains("[NEXT ACT]");
                system.append(actSpecifiesCondition ? NEXT_ACT_RULE_CONDITIONAL : NEXT_ACT_RULE_DEFAULT)
                      .append("\n\n");
            }
        }
        if (summary != null && !summary.isBlank()) {
            system.append("STORY SO FAR:\n").append(summary).append("\n\n");
        }
        system.append(SYSTEM_FORMATTING);

        String characterLabel = characterLabel(scenario);
        StringBuilder user = new StringBuilder();
        if (!recentTurns.isEmpty()) {
            user.append("Recent exchange:\n").append(transcript(recentTurns, characterLabel)).append('\n');
        }
        user.append("Player: ").append(input.formattedLine()).append("\n\n").append(characterLabel).append(":");

        return new ChatPrompt(system.toString(), user.toString());
    }

    /** The character's name if character.txt declared one (see ChatScenario), "Character" otherwise. */
    public static String characterLabel(ChatScenario scenario) {
        String name = scenario.characterName();
        return name == null || name.isBlank() ? "Character" : name;
    }

    /** "Player: .../{characterLabel}: .../Narration: ..." transcript, shared with ChatSummarizer folding. */
    public static String transcript(List<ChatTurn> turns, String characterLabel) {
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : turns) {
            sb.append(switch (turn.speaker()) {
                case PLAYER   -> "Player: ";
                case LLM      -> characterLabel + ": ";
                case NARRATOR -> "Narration: ";
            }).append(turn.text()).append('\n');
        }
        return sb.toString();
    }
}
