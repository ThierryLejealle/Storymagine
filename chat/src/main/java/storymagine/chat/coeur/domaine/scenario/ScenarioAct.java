package storymagine.chat.coeur.domaine.scenario;

import storymagine.chat.coeur.domaine.activity.ActivityDirective;

import java.util.List;

/**
 * One node of a ChatScenario's act tree, already resolved to a leaf — only leaves are ever
 * "current" (see ChatFileStorageAdapter / ScenarioOutlineParser for how the single-file, nested
 * outline is flattened). In the authors' vocabulary a leaf is a "played scene" (the deepest heading
 * of a branch, the only kind ever played) and a heading with sub-headings is a "container" (never
 * played alone, its body is inherited by the played scenes below it). A played scene's text holds only
 * the situation now and what each NPC does in that scene, never what comes later (anti-leak rule) ;
 * "{private Name}" lines in it are filtered per NPC when the prompt is built (see PrivateLines). number is the dotted outline position (1, 1.1, 1.1.1...) deduced from
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
 * the "{npc-delta ...}" changes (see CharacterDelta) written in this leaf or in an ancestor entered
 * for the first time by this transition, lines already cut out of text : the sheets in force at act
 * N are the Cast's plus the deltas of acts 1..N in order (see ChatScenario.sheetAt). activityPath
 * holds the activity commands (see ActivityDirective) of every heading on the way from the root to
 * this leaf, one list per heading, root first — unlike beats and deltas the WHOLE path is kept, since
 * the activity in force here is computed from it (see ChatScenario.activityAt). stories are the
 * "{npc-story ...}" commands (see StoryDirective) written in this leaf or in an ancestor entered for the
 * first time by this transition, like deltas : the tags in force at act N come from the stories of acts
 * 1..N in order (see StoryResolver). sceneStyle is the extra style advice of the nearest heading on the way
 * from the root to this leaf that wrote a "{style name}" (NONE if none did) : it is added after style.txt
 * while this leaf is current (see ChatScenario.styleAt).
 */
public record ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening,
                          List<CharacterDelta> deltas, List<List<ActivityDirective>> activityPath,
                          List<StoryDirective> stories, ScenarioStyle sceneStyle) {

    public ScenarioAct {
        sceneStyle = sceneStyle == null ? ScenarioStyle.NONE : sceneStyle;
        deltas = deltas == null ? List.of() : List.copyOf(deltas);
        activityPath = activityPath == null ? List.of() : activityPath.stream().map(List::copyOf).toList();
        stories = stories == null ? List.of() : List.copyOf(stories);
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening,
                       List<CharacterDelta> deltas, List<List<ActivityDirective>> activityPath,
                       List<StoryDirective> stories) {
        this(number, title, text, beats, opening, deltas, activityPath, stories, ScenarioStyle.NONE);
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening,
                       List<CharacterDelta> deltas, List<List<ActivityDirective>> activityPath) {
        this(number, title, text, beats, opening, deltas, activityPath, List.of());
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening,
                       List<CharacterDelta> deltas) {
        this(number, title, text, beats, opening, deltas, List.of());
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats, NpcOpening opening) {
        this(number, title, text, beats, opening, List.of());
    }

    /**
     * Number of the heading this leaf hangs from ("" when it is a top-level heading) : what a level
     * forced by the player is attached to (see ForcedActivity).
     */
    public String parentAnchor() {
        List<Integer> segments = number.segments();
        return segments.size() <= 1 ? "" : new ActNumber(segments.subList(0, segments.size() - 1)).display();
    }

    /** True if this leaf is under the heading numbered anchor ("" = the root, which holds everything). */
    public boolean isUnder(String anchor) {
        return anchor.isEmpty() || number.display().startsWith(anchor + ".");
    }

    public ScenarioAct(ActNumber number, String title, String text, List<String> beats) {
        this(number, title, text, beats, NpcOpening.NONE);
    }

    /** Convenience for flat, single-level acts (tests, simple scenarios) : beats = Teaser.extractAll(text). */
    public static ScenarioAct leaf(ActNumber number, String title, String text) {
        return new ScenarioAct(number, title, text, Teaser.extractAll(text));
    }
}
