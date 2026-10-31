package storymagine.chat.coeur.domaine.activity;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.Npc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The inheritance rules of the activity commands, root heading to leaf : who is explicit, who a nameless command touches. */
class ActivityResolverTest {

    private static final Cast CAST = new Cast(List.of(
        new Npc("j1", "J1", "sheet", ""), new Npc("j2", "J2", "sheet", ""),
        new Npc("j3", "J3", "sheet", ""), new Npc("j4", "J4", "sheet", "")));

    private static ActivityDirective level(NpcActivity level, String names) {
        return ActivityDirective.level(level, names);
    }

    @SafeVarargs
    private static CastActivity resolve(List<ActivityDirective>... headings) {
        return ActivityResolver.resolve(CAST, List.of(headings));
    }

    private static NpcActivityState state(NpcActivity level, ActivitySource source) {
        return new NpcActivityState(level, source);
    }

    @Test
    void withoutAnyCommandEveryoneIsPresentAndImplicit() {
        CastActivity activity = resolve();

        for (String id : List.of("j1", "j2", "j3", "j4")) assertEquals(NpcActivityState.DEFAULT, activity.stateOf(id));
    }

    @Test
    void aNamelessCommandAppliesToEveryoneNotNamedInTheSameHeading() {
        // l'exemple du cahier des charges : J1 actif, J3 passif, puis {npc-absent} sans nom => J2 et J4
        CastActivity activity = resolve(List.of(
            level(NpcActivity.ACTIVE, "J1"), level(NpcActivity.PASSIVE, "J3"), level(NpcActivity.ABSENT, "")));

        assertEquals(state(NpcActivity.ACTIVE, ActivitySource.EXPLICIT), activity.stateOf("j1"));
        assertEquals(state(NpcActivity.ABSENT, ActivitySource.IMPLICIT), activity.stateOf("j2"));
        assertEquals(state(NpcActivity.PASSIVE, ActivitySource.EXPLICIT), activity.stateOf("j3"));
        assertEquals(state(NpcActivity.ABSENT, ActivitySource.IMPLICIT), activity.stateOf("j4"));
    }

    @Test
    void allNamesEveryoneExplicitly() {
        CastActivity activity = resolve(List.of(level(NpcActivity.ACTIVE, "*")));

        for (String id : List.of("j1", "j2", "j3", "j4")) {
            assertEquals(state(NpcActivity.ACTIVE, ActivitySource.EXPLICIT), activity.stateOf(id));
        }
    }

    @Test
    void allIsExplicitSoItSurvivesANamelessCommandOfTheSameOrAChildHeading() {
        CastActivity activity = resolve(
            List.of(level(NpcActivity.ACTIVE, "*"), level(NpcActivity.ABSENT, "")),
            List.of(level(NpcActivity.PASSIVE, "")));

        for (String id : List.of("j1", "j2", "j3", "j4")) assertEquals(NpcActivity.ACTIVE, activity.levelOf(id));
    }

    @Test
    void aNameWrittenNextToAllIsMoreSpecific() {
        CastActivity activity = resolve(List.of(level(NpcActivity.PASSIVE, "J2"), level(NpcActivity.ACTIVE, "*")));

        assertEquals(NpcActivity.ACTIVE, activity.levelOf("j1"));
        assertEquals(NpcActivity.PASSIVE, activity.levelOf("j2"));
        assertEquals(NpcActivity.ACTIVE, activity.levelOf("j4"));
    }

    @Test
    void allOfAChildHeadingOverridesTheParent() {
        CastActivity activity = resolve(
            List.of(level(NpcActivity.ABSENT, "J1"), level(NpcActivity.PASSIVE, "J2")),
            List.of(level(NpcActivity.ACTIVE, "*")));

        for (String id : List.of("j1", "j2", "j3", "j4")) assertEquals(NpcActivity.ACTIVE, activity.levelOf(id));
    }

    @Test
    void twoAllCommandsInOneHeadingOrAllMixedWithNamesAreRefused() {
        assertThrows(IllegalArgumentException.class,
            () -> resolve(List.of(level(NpcActivity.ACTIVE, "*"), level(NpcActivity.PASSIVE, "*"))));
        assertThrows(IllegalArgumentException.class, () -> ActivityResolver.resolveNames(CAST, "* J1"));
    }

    @Test
    void theOrderTheCommandsAreWrittenInNeverMatters() {
        CastActivity nameless = resolve(List.of(level(NpcActivity.ABSENT, ""), level(NpcActivity.ACTIVE, "J1")));

        assertEquals(NpcActivity.ACTIVE, nameless.levelOf("j1"));
        assertEquals(NpcActivity.ABSENT, nameless.levelOf("j2"));
    }

    @Test
    void anExplicitLevelOfAParentSurvivesANamelessCommandOfAChild() {
        CastActivity activity = resolve(
            List.of(level(NpcActivity.ACTIVE, "J1"), level(NpcActivity.ABSENT, "")),
            List.of(level(NpcActivity.PASSIVE, "")));

        assertEquals(state(NpcActivity.ACTIVE, ActivitySource.EXPLICIT), activity.stateOf("j1"), "explicite : le sans-nom ne le touche pas");
        assertEquals(state(NpcActivity.PASSIVE, ActivitySource.IMPLICIT), activity.stateOf("j2"), "implicite : le sans-nom du fils l'emporte");
    }

    @Test
    void aNamedCommandOfAChildOverridesTheParent() {
        CastActivity activity = resolve(
            List.of(level(NpcActivity.ACTIVE, "J1")),
            List.of(level(NpcActivity.ABSENT, "J1")));

        assertEquals(state(NpcActivity.ABSENT, ActivitySource.EXPLICIT), activity.stateOf("j1"));
    }

    @Test
    void aHeadingWithoutCommandInheritsEverythingFromItsParent() {
        CastActivity activity = resolve(List.of(level(NpcActivity.ABSENT, "J2")), List.of());

        assertEquals(NpcActivity.ABSENT, activity.levelOf("j2"));
    }

    @Test
    void resetPutsEveryoneBackToPresentAndExplicit() {
        CastActivity activity = resolve(
            List.of(level(NpcActivity.ABSENT, "J1"), level(NpcActivity.PASSIVE, "")),
            List.of(ActivityDirective.reset(), level(NpcActivity.ACTIVE, "")));

        for (String id : List.of("j1", "j2", "j3", "j4")) {
            assertEquals(state(NpcActivity.PRESENT, ActivitySource.EXPLICIT), activity.stateOf(id),
                id + " : tout le monde est explicite apres reset, le sans-nom du meme titre ne touche personne");
        }
    }

    @Test
    void namesMayHaveSeveralWordsAndAreSeparatedBySpaces() {
        Cast cast = new Cast(List.of(new Npc("celeste", "Céleste Dubois", "sheet", ""), new Npc("marcus", "Marcus", "sheet", "")));

        List<Npc> found = ActivityResolver.resolveNames(cast, "Céleste Dubois Marcus");

        assertEquals(List.of("celeste", "marcus"), found.stream().map(Npc::id).toList());
    }

    @Test
    void aNameIsMatchedWhateverTheCaseAndAccents() {
        assertEquals(List.of("j1", "j3"), ActivityResolver.resolveNames(CAST, "j1 J3").stream().map(Npc::id).toList());
    }

    @Test
    void anUnknownNameIsAnError() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> resolve(List.of(level(NpcActivity.ACTIVE, "J1 Zorglub"))));

        assertTrue(error.getMessage().contains("Zorglub"), error.getMessage());
    }

    @Test
    void namingTheSameCharacterTwiceInOneHeadingIsAnError() {
        assertThrows(IllegalArgumentException.class,
            () -> resolve(List.of(level(NpcActivity.ACTIVE, "J1"), level(NpcActivity.ABSENT, "J1 J2"))));
    }

    @Test
    void twoNamelessCommandsInOneHeadingAreAnError() {
        assertThrows(IllegalArgumentException.class,
            () -> resolve(List.of(level(NpcActivity.ACTIVE, ""), level(NpcActivity.ABSENT, ""))));
    }
}
