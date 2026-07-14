package storymagine.chat;

import storymagine.chat.coeur.domaine.agent.chatsummarizer.ChatSummarizer;
import storymagine.chat.coeur.domaine.agent.clarityreviewer.ScenarioClarityReviewer;
import storymagine.chat.coeur.domaine.agent.continuityreviewer.ScenarioContinuityReviewer;
import storymagine.chat.coeur.domaine.agent.nextactreadiness.NextActReadinessAnalyst;
import storymagine.chat.coeur.domaine.agent.npcmindstate.NpcMindStateAnalyst;
import storymagine.chat.coeur.domaine.agent.roleplaynarrator.RoleplayNarrator;
import storymagine.chat.coeur.ports.ChatStoragePort;
import storymagine.chat.coeur.service.ChatService;
import storymagine.chat.coeur.service.ChatServiceImpl;
import storymagine.chat.coeur.service.ScenarioTesterService;
import storymagine.chat.coeur.service.ScenarioTesterServiceImpl;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;

/**
 * Wires the chat domain with its infrastructure adapters. One ModelCallPort (/api/chat) drives
 * both the roleplay turn (RoleplayNarrator) and the ChatSummarizer archivist call.
 */
public class ChatModule {

    public static ChatService assemble(ModelCallPort llm) {
        return assemble(llm, new ChatFileStorageAdapter(), LogPort.NOOP);
    }

    public static ChatService assemble(ModelCallPort llm, ChatStoragePort storage) {
        return assemble(llm, storage, LogPort.NOOP);
    }

    public static ChatService assemble(ModelCallPort llm, ChatStoragePort storage, LogPort log) {
        var roleplayNarrator        = new RoleplayNarrator(llm);
        var summarizer              = new ChatSummarizer(llm);
        var nextActReadinessAnalyst = new NextActReadinessAnalyst(llm);
        var npcMindStateAnalyst     = new NpcMindStateAnalyst(llm);
        return new ChatServiceImpl(storage, roleplayNarrator, summarizer, nextActReadinessAnalyst,
            npcMindStateAnalyst, log);
    }

    public static ScenarioTesterService assembleTester(ModelCallPort llm, LogPort log) {
        var continuityReviewer = new ScenarioContinuityReviewer(llm);
        var clarityReviewer    = new ScenarioClarityReviewer(llm);
        return new ScenarioTesterServiceImpl(continuityReviewer, clarityReviewer, log);
    }
}
