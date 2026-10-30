package storymagine.chat.coeur.domaine.agent.chatsummarizer;

import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;

/** language : the scenario's, so the summary is written in the language of the chat. */
public record ChatSummarizerInput(String previousSummary, String transcriptToFold, ScenarioLanguage language) {
}
