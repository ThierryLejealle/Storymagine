package storymagine.chat.coeur.domaine.scenario;

import java.util.List;

/**
 * One node of a ChatScenario's act tree, already resolved to a leaf — only leaves are ever
 * "current" (see ChatFileStorageAdapter / ScenarioOutlineParser for how the single-file, nested
 * outline is flattened). number is the dotted outline position (1, 1.1, 1.1.1...) deduced from
 * the document, never typed by the author. title is this leaf's own heading text — display-only,
 * never sent to the LLM. text is the RESOLVED prompt content : this leaf's own body prefixed by
 * every ancestor's own body (root-to-leaf order) — that's how a parent's content is inherited by
 * its descendants. beats are the "[...]" story-beat lines (see Teaser) to replay as NARRATOR turns
 * when this leaf becomes current : this leaf's own beats plus those of any ancestor entered for
 * the first time by this transition (an ancestor already active before this transition does not
 * repeat its beats) — this is why beats can't simply be re-derived from text via Teaser.extractAll
 * at the point of use, unlike before. opening is who speaks first when this leaf becomes current
 * (see NpcOpening) : like beats, it comes from the leaf's own body or from an ancestor entered for
 * the first time by this transition ; the marker line itself is already cut out of text. deltas are
 * the "{delta-pnj ...}" changes (see CharacterDelta) written in this leaf or in an ancestor entered
 * for the first time by this transition, lines already cut out of text : the sheets in force at act
 * N are the Cast's plus the deltas of acts 1..N in order (see ChatScenario.sheetAt).
 */
public record ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening,
                          List<CharacterDelta> deltas) {

    public ScenarioAct {
        deltas = deltas == null ? List.of() : List.copyOf(deltas);
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening) {
        this(number, title, text, beats, opening, List.of());
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats) {
        this(number, title, text, beats, NpcOpening.NONE);
    }

    /** Convenience for flat, single-level acts (tests, simple scenarios) : beats = Teaser.extractAll(text). */
    public static ScenarioAct leaf(ActNumber number, String title, String text) {
        return new ScenarioAct(number, title, text, Teaser.extractAll(text));
    }
}
