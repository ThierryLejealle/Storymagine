package storymagine.chat.coeur.domaine.agent.chatsummarizer;

import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.ModelCallPort;

/**
 * Folds older turns of a chat session into the running summary, so the conversation can continue
 * past the model's context window without losing established facts. See ChatSummarizer.md.
 */
public class ChatSummarizer {

    private static final String AGENT_NAME = "ChatSummarizer";

    private final ModelCallPort llm;

    public ChatSummarizer(ModelCallPort llm) {
        this.llm = llm;
    }

    public String agentName() { return AGENT_NAME; }

    public int contextWindow() { return llm.contextWindow(); }

    public ChatSummarizerOutput call(ChatSummarizerInput input) {
        String system = """
            You summarize a roleplay chat between the player and one or more characters, each
            labeled by name in the turns below. Fold the new turns into the previous summary and
            produce one updated summary.

            How to read the turns:
            - Text between *asterisks* is physical action or scene detail, not speech.
            - Lines starting with "OOC:" are the player speaking outside the story, often to
              agree with you on what should happen next.
            - Lines starting with "DO:" hand narration to the assistant; treat the events they
              introduce as story events.

            Keep:
            - Established facts about the characters, their relationship, and the world.
            - Events, decisions, and promises that still matter now.
            - The current situation: where the characters are and what they are doing.
            - Any plan agreed in OOC lines, as one sentence starting with "Pending:". Example: an
              OOC exchange agreeing the two characters will sneak into the archive at nightfall
              becomes "Pending: they plan to sneak into the archive at nightfall."

            Drop:
            - Exact wording, small talk, and momentary reactions.
            - Any state that a later turn replaced; keep only the latest version.

            Rules:
            - Use only the previous summary and the turns; add nothing else.
            - If the previous summary is "(empty)", build the summary from the turns alone.
            - Write factual prose in the same language as the turns, third person: past tense for
              events, present tense for the current situation.
            - Output only the summary text, with no heading and no commentary.""";

        String user = "PREVIOUS SUMMARY:\n" + emptyIfBlank(input.previousSummary())
            + "\n\nNEW TURNS:\n" + input.transcriptToFold()
            + "\n\nUPDATED SUMMARY:";

        String raw = llm.generate(system, user, 0.4, LlmCallContext.of(agentName()).withThink(true)).text();
        return new ChatSummarizerOutput(raw.strip());
    }

    private static String emptyIfBlank(String s) {
        return (s == null || s.isBlank()) ? "(empty)" : s;
    }
}
