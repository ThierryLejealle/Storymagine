package storymagine.chat.ui.web;

import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.GenerationSettings;

import java.util.List;

/**
 * JSON shape served by ChatWebServer — the page's only contract with the backend. promptTokens is
 * an estimate (chars/4, see ChatContextBudget) of the prompt the NEXT turn would send given the
 * session's state right now, not a real measurement — computed locally so it reflects reality
 * immediately, including any compaction that just happened ; contextWindow is the model's total
 * capacity — together they drive the context-usage gauge. currentAct/totalActs are 0 when the
 * scenario has no acts ; actAdvanced is true only on the response right after a transition (button
 * click or "[NEXT ACT]" marker), never on a plain reload. turns is the FULL current turn window —
 * used only to seed the display on page load/reload. newTurns is what this exchange actually
 * added (player + reply + any NARRATOR beats from an act transition, see Teaser) — empty on a
 * plain GET /history ; the UI appends these to what it already shows instead of repainting from
 * turns, so a compaction (which shrinks turns) never makes already-seen history vanish from
 * screen. removedTurnCount is how many already-displayed trailing bubbles the UI must remove
 * FIRST, before appending newTurns — 0 for a normal exchange, 1 for POST /retry (the reply it
 * replaces), N for POST /undo (everything from the last player turn onward, newTurns then empty).
 * actTitle is the current act's display-only "# ..." title (see ScenarioAct), null when no
 * act is active — deliberately only ever the current one, never the full list, so later acts'
 * titles don't spoil what's coming. generationSettings is the session's current overrides (see
 * GenerationSettings) — null fields mean "using RoleplayNarrator's default" — used to pre-fill the
 * settings panel on page load.
 */
public record ChatHistoryView(String scenarioName, List<ChatTurn> turns, List<ChatTurn> newTurns,
                               int removedTurnCount, boolean compacted, int promptTokens, int contextWindow,
                               int currentAct, int totalActs, boolean actAdvanced, String actTitle,
                               GenerationSettings generationSettings) {
}
