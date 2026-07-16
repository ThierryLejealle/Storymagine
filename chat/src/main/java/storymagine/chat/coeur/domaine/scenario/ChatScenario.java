package storymagine.chat.coeur.domaine.scenario;

import java.util.List;

/**
 * Static roleplay setup read from chatscenarios/{name}/ : the Cast of every Npc the LLM can play,
 * the (evergreen) premise, and an optional ordered list of progressive acts. When acts are
 * present, only the current one is sent to the LLM alongside the premise — never the whole list —
 * both to keep the prompt small and to make later acts physically absent from the model's context
 * (a stronger anti-spoiler guarantee than asking it not to act on what it can already see).
 * cast comes from every ".txt" file in the scenario's directory except scenario.txt itself — one
 * Npc per file, its filename (minus extension) as id (see ChatFileStorageAdapter) — so a legacy
 * scenario with just a single character.txt naturally produces a one-Npc Cast, no special case.
 * playerName is what the transcript/stop-sequences call the human player instead of the generic
 * "Player" (see ChatPromptBuilder, RoleplayNarrator) — read from an optional "Joueur : ..." first
 * line of scenario.txt (see ChatFileStorageAdapter) ; null/blank always normalizes to "Alex" here,
 * so every ChatScenario in the codebase (including test fixtures using the 4-arg constructor) has
 * a concrete name to work with, never a blank one.
 */
public record ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName) {

    public ChatScenario {
        if (playerName == null || playerName.isBlank()) playerName = "Alex";
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts) {
        this(name, cast, premise, acts, null);
    }
}
