package storymagine.chat.coeur.domaine.scenario;

import java.util.List;

/**
 * Static roleplay setup read from chatscenarios/{name}/ : the Cast of every Npc the LLM can play,
 * the (evergreen) premise, and an optional ordered list of progressive acts. When acts are
 * present, only the current one is sent to the LLM alongside the premise — never the whole list —
 * both to keep the prompt small and to make later acts physically absent from the model's context
 * (a stronger anti-spoiler guarantee than asking it not to act on what it can already see).
 * cast comes from every character sheet in the scenario's characters/ directory — one Npc per
 * file, its filename (minus extension) as id (see ChatFileStorageAdapter) — so a scenario with a
 * single sheet naturally produces a one-Npc Cast, no special case.
 * playerName is what the transcript/stop-sequences call the human player instead of the generic
 * "Player" (see ChatPromptBuilder, RoleplayNarrator) — read from an optional "Joueur : ..." first
 * line of scenario.txt (see ChatFileStorageAdapter) ; null/blank always normalizes to "Alex" here,
 * so every ChatScenario in the codebase (including test fixtures using the 4-arg constructor) has
 * a concrete name to work with, never a blank one.
 * storyOpening is who speaks first when the story begins, from a "[NPC STARTS]" line in the
 * premise (see NpcOpening) ; null means NONE. Every Npc named by it, or by an act's opening, must
 * exist in the cast : an author's typo fails loudly here instead of silently picking someone else.
 */
public record ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName,
                           NpcOpening storyOpening) {

    public ChatScenario {
        if (playerName == null || playerName.isBlank()) playerName = "Alex";
        if (storyOpening == null) storyOpening = NpcOpening.NONE;
        requireKnownNpc(cast, storyOpening, "l'ouverture du scenario");
        for (ScenarioAct act : acts) requireKnownNpc(cast, act.opening(), "l'acte " + act.number());
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName) {
        this(name, cast, premise, acts, playerName, NpcOpening.NONE);
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts) {
        this(name, cast, premise, acts, null, NpcOpening.NONE);
    }

    /**
     * Who speaks first once act `act` has just begun (0 = no acts in use). The story-level opening
     * goes first for act 0/1 — the story and its first act begin at the same moment.
     */
    public NpcOpening openingOfAct(int act) {
        if (act <= 1 && !storyOpening.isNone()) return storyOpening;
        return act >= 1 && act <= acts.size() ? acts.get(act - 1).opening() : NpcOpening.NONE;
    }

    private static void requireKnownNpc(Cast cast, NpcOpening opening, String where) {
        if (opening.isNamed() && cast.findByName(opening.npcName()).isEmpty()) {
            throw new IllegalArgumentException("[NPC STARTS: " + opening.npcName() + "] dans " + where
                + " : aucun personnage de ce nom dans characters/.");
        }
    }
}
