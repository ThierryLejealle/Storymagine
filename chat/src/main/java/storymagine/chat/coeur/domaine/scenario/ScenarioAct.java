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
 * at the point of use, unlike before.
 */
public record ScenarioAct(ActNumber number, String title, String text, List<String> beats) {

    /** Convenience for flat, single-level acts (tests, simple scenarios) : beats = Teaser.extractAll(text). */
    public static ScenarioAct leaf(ActNumber number, String title, String text) {
        return new ScenarioAct(number, title, text, Teaser.extractAll(text));
    }
}
