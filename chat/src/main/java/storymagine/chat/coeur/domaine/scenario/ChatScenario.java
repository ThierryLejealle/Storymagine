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
 * storyOpening is who speaks first when the story begins, from a "{npc-starts}" line in the
 * premise (see NpcOpening) ; null means NONE. Every Npc named by it, or by an act's opening, must
 * exist in the cast : an author's typo fails loudly here instead of silently picking someone else.
 * language is the language the Npcs write in, from an optional "Langue : ..." header line next to
 * "Joueur : ..." ; null means UNSPECIFIED. style is the optional advice of style.txt, shared by every
 * Npc (see ScenarioStyle) ; null means NONE.
 */
public record ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName,
                           NpcOpening storyOpening, ScenarioLanguage language, ScenarioStyle style) {

    public ChatScenario {
        if (playerName == null || playerName.isBlank()) playerName = "Alex";
        if (storyOpening == null) storyOpening = NpcOpening.NONE;
        if (language == null) language = ScenarioLanguage.UNSPECIFIED;
        if (style == null) style = ScenarioStyle.NONE;
        requireKnownNpc(cast, storyOpening, "l'ouverture du scenario");
        for (ScenarioAct act : acts) {
            requireKnownNpc(cast, act.opening(), "l'acte " + act.number());
            for (CharacterDelta delta : act.deltas()) {
                if (cast.findByName(delta.npcName()).isEmpty()) {
                    throw new IllegalArgumentException("{delta-pnj " + delta.npcName() + "} dans l'acte " + act.number()
                        + " : aucun personnage de ce nom dans characters/.");
                }
            }
        }
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName,
                        NpcOpening storyOpening, ScenarioLanguage language) {
        this(name, cast, premise, acts, playerName, storyOpening, language, ScenarioStyle.NONE);
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName,
                        NpcOpening storyOpening) {
        this(name, cast, premise, acts, playerName, storyOpening, ScenarioLanguage.UNSPECIFIED);
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts, String playerName) {
        this(name, cast, premise, acts, playerName, NpcOpening.NONE, ScenarioLanguage.UNSPECIFIED);
    }

    public ChatScenario(String name, Cast cast, String premise, List<ScenarioAct> acts) {
        this(name, cast, premise, acts, null, NpcOpening.NONE, ScenarioLanguage.UNSPECIFIED);
    }

    /**
     * Who speaks first once act `act` has just begun (0 = no acts in use). The story-level opening
     * goes first for act 0/1 — the story and its first act begin at the same moment.
     */
    public NpcOpening openingOfAct(int act) {
        if (act <= 1 && !storyOpening.isNone()) return storyOpening;
        return act >= 1 && act <= acts.size() ? acts.get(act - 1).opening() : NpcOpening.NONE;
    }

    /**
     * npc as it is at act `act` (1-based ; 0 = no acts in use) : its sheet plus every delta written in
     * acts 1..act that targets it, in order. Computed on demand from the acts, never stored, so
     * rolling back, going to the previous act, loading a save or reloading the scenario need no
     * special handling.
     */
    public Npc sheetAt(Npc npc, int act) {
        Npc result = npc;
        for (int i = 0; i < Math.min(act, acts.size()); i++) {
            for (CharacterDelta delta : acts.get(i).deltas()) {
                boolean targetsNpc = cast.findByName(delta.npcName()).map(n -> n.id().equals(npc.id())).orElse(false);
                if (!targetsNpc) continue;
                result = delta.visibility() == CharacterDelta.Visibility.PUBLIC
                    ? result.withPublicAddition(delta.text())
                    : result.withSecretAddition(delta.text());
            }
        }
        return result;
    }

    private static void requireKnownNpc(Cast cast, NpcOpening opening, String where) {
        if (opening.isNamed() && cast.findByName(opening.npcName()).isEmpty()) {
            throw new IllegalArgumentException("{npc-starts " + opening.npcName() + "} dans " + where
                + " : aucun personnage de ce nom dans characters/.");
        }
    }
}
