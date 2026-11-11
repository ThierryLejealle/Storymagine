package storymagine.chat.coeur.domaine.activity;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.CastInstantiator;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.NpcOnlyMarker;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.StartingRoster;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.ScenarioLanguage;
import storymagine.chat.coeur.domaine.scenario.ScenarioStyle;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Characters outside the story : {npc-only} at the start, {npc-arrives} / {npc-leaves} per act, and sheets used as templates. */
class NpcArrivalsTest {

    private static Npc sheet(String id, String name) {
        return new Npc(id, name, "# " + name + "\nPublic of " + name + ".", "Secret of " + name + ".");
    }

    private static final Cast CAST = new Cast(List.of(sheet("elena", "Elena"), sheet("marc", "Marc"), sheet("orlo", "Orlo")));

    private static List<ActivityDirective> arrives(String names) { return List.of(ActivityDirective.arrives(names)); }

    // ── the activity of an arrival ─────────────────────────────────────────

    @Test
    void aCharacterOutsideAtTheStartArrivesAtTheActOfItsCommandAndStaysAfter() {
        Set<String> outside = Set.of("orlo");
        var before = ActivityResolver.resolve(CAST, List.of(List.of()), outside);
        var arrival = ActivityResolver.resolve(CAST, List.of(arrives("Orlo")), outside);

        assertTrue(before.levelOf("orlo").isOutside());
        assertEquals(Set.of("elena", "marc"), before.inSceneIds());
        assertEquals(NpcActivityState.DEFAULT, arrival.stateOf("orlo"));
        assertEquals(Set.of("elena", "marc", "orlo"), arrival.inSceneIds());
    }

    @Test
    void leavesPutsTheCharacterBackOutsideAndALaterHeadingCanBringItBack() {
        var gone = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.leaves("Marc"))));
        var back = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.leaves("Marc")), arrives("Marc")));

        assertEquals(Set.of("marc"), gone.outsideIds());
        assertTrue(back.outsideIds().isEmpty());
    }

    @Test
    void broadCommandsNeverReachACharacterOutsideTheStory() {
        Set<String> outside = Set.of("orlo");
        var everybodyActive = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.level(NpcActivity.ACTIVE, "*"))), outside);
        var nameless = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.level(NpcActivity.PASSIVE, ""))), outside);
        var reset = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.reset())), outside);

        for (CastActivity activity : List.of(everybodyActive, nameless, reset)) {
            assertTrue(activity.levelOf("orlo").isOutside());
        }
        assertEquals(NpcActivity.ACTIVE, everybodyActive.levelOf("elena"));
    }

    @Test
    void anActivityLevelAfterTheArrivalInTheSameHeadingAppliesToTheNewcomer() {
        var activity = ActivityResolver.resolve(CAST,
            List.of(List.of(ActivityDirective.level(NpcActivity.ACTIVE, "Orlo"), ActivityDirective.arrives("Orlo"))), Set.of("orlo"));

        assertEquals(new NpcActivityState(NpcActivity.ACTIVE, ActivitySource.EXPLICIT), activity.stateOf("orlo"));
    }

    @Test
    void anActivityLevelForSomeoneNotInTheStoryYetIsRefused() {
        var error = assertThrows(IllegalArgumentException.class, () -> ActivityResolver.resolve(CAST,
            List.of(List.of(ActivityDirective.level(NpcActivity.ACTIVE, "Orlo"))), Set.of("orlo")));

        assertTrue(error.getMessage().contains("{npc-arrives Orlo}"), error.getMessage());
    }

    @Test
    void anArrivalOfSomeoneAlreadyThereChangesNothing() {
        var activity = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.level(NpcActivity.PASSIVE, "Elena")), arrives("Elena")));

        assertEquals(new NpcActivityState(NpcActivity.PASSIVE, ActivitySource.EXPLICIT), activity.stateOf("elena"));
    }

    // ── the player's click never reaches an outsider ───────────────────────

    @Test
    void theClickCycleSkipsOutsideEntirely() {
        assertEquals(NpcActivity.ACTIVE, NpcActivity.ABSENT.next());
        assertEquals(NpcActivity.ACTIVE, NpcActivity.OUTSIDE.next());
        assertFalse(NpcActivity.OUTSIDE.isInScene());
    }

    // ── commands in the text ───────────────────────────────────────────────

    @Test
    void arrivesAndLeavesAreCutOutOfTheTextWhenAloneOnTheirLine() {
        var extracted = ActivityDirectiveMarker.extract("""
            The market opens.
            {npc-arrives goblin Grik, Snak}
            {npc-leaves Orlo}
            Night falls.""");

        assertEquals("The market opens.\nNight falls.", extracted.text().strip());
        assertEquals(List.of(ActivityDirective.Kind.ARRIVES, ActivityDirective.Kind.LEAVES),
            extracted.directives().stream().map(ActivityDirective::kind).toList());
        assertEquals("goblin Grik, Snak", extracted.directives().get(0).namesText());
    }

    @Test
    void leavesAcceptsSeveralNamesSeparatedByCommas() {
        var gone = ActivityResolver.resolve(CAST, List.of(List.of(ActivityDirective.leaves("Marc, Orlo"))));

        assertEquals(Set.of("marc", "orlo"), gone.outsideIds());
    }

    @Test
    void anArrivesInTheMiddleOfASentenceIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> ActivityDirectiveMarker.extract("He said {npc-arrives Orlo} loudly."));
    }

    @Test
    void npcOnlyIsCutOutAndItsNamesKept() {
        var extracted = NpcOnlyMarker.extract("Premise.\n{npc-only Elena, Marc}\nMore.");

        assertEquals(List.of("Elena, Marc"), extracted.namesTexts());
        assertEquals("Premise.\nMore.", extracted.text().strip());
        assertThrows(IllegalArgumentException.class, () -> NpcOnlyMarker.extract("Mid {npc-only Elena} sentence."));
    }

    // ── templates ──────────────────────────────────────────────────────────

    private static final Npc GOBLIN = new Npc("goblin", "Goblin", "# Goblin\n%NPC% snarls and guards the road.", "%NPC% hates the player.");

    @Test
    void aSheetUsedWithANameIsATemplateAndEachNameCreatesACharacter() {
        Cast cast = CastInstantiator.assemble(List.of(sheet("elena", "Elena"), GOBLIN),
            List.of("goblin Grik, Snak", "goblin Grik"));

        assertEquals(List.of("elena", "grik", "snak"), cast.npcs().stream().map(Npc::id).toList());
        Npc grik = cast.find("grik").orElseThrow();
        assertEquals("Grik", grik.name());
        assertEquals("# Grik\nGrik snarls and guards the road.", grik.publicInfo());
        assertEquals("Grik hates the player.", grik.secretInfo());
        assertTrue(cast.isTemplate("goblin"));
        assertTrue(cast.find("goblin").isEmpty());
    }

    @Test
    void aPlainArrivalNeedsNoTemplateAndCreatesNothing() {
        Cast cast = CastInstantiator.assemble(List.of(sheet("elena", "Elena"), sheet("marc", "Marc")), List.of("Marc"));

        assertEquals(2, cast.size());
        assertTrue(cast.templateIds().isEmpty());
    }

    @Test
    void aSheetCannotBeBothACharacterAndATemplate() {
        var error = assertThrows(IllegalArgumentException.class, () -> CastInstantiator.assemble(
            List.of(GOBLIN), List.of("goblin Grik", "goblin")));

        assertTrue(error.getMessage().contains("modele"), error.getMessage());
    }

    @Test
    void aNameAlreadyTakenByAnotherCharacterIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> CastInstantiator.assemble(
            List.of(sheet("elena", "Elena"), GOBLIN), List.of("goblin Elena")));
    }

    @Test
    void theSameNameForTwoTemplatesIsRefused() {
        Npc merchant = new Npc("merchant", "Merchant", "# Merchant\n%NPC% sells.", "");

        assertThrows(IllegalArgumentException.class, () -> CastInstantiator.assemble(
            List.of(GOBLIN, merchant), List.of("goblin Grik", "merchant Grik")));
    }

    @Test
    void anArrivalOfATemplateResolvesToTheCreatedCharacters() {
        Cast cast = CastInstantiator.assemble(List.of(sheet("elena", "Elena"), GOBLIN), List.of("goblin Grik, Snak"));
        var activity = ActivityResolver.resolve(cast, List.of(arrives("goblin Grik, Snak")), Set.of("grik", "snak"));

        assertEquals(Set.of("elena", "grik", "snak"), activity.inSceneIds());
    }

    // ── the scenario as a whole ────────────────────────────────────────────

    private static ChatScenario scenario(StartingRoster roster, ScenarioAct... acts) {
        return new ChatScenario("test", CAST, "Premise.", List.of(acts), null, NpcOpening.NONE,
            ScenarioLanguage.UNSPECIFIED, ScenarioStyle.NONE, roster);
    }

    private static ScenarioAct act(int n, List<ActivityDirective> directives) {
        return new ScenarioAct(ActNumber.of(n), "Act " + n, "text", List.of(), NpcOpening.NONE, List.<CharacterDelta>of(), List.of(directives));
    }

    @Test
    void onlyTheNamedCharactersStartAndTheOthersArriveWithTheirAct() {
        ChatScenario scenario = scenario(new StartingRoster("Elena, Marc"), act(1, List.of()), act(2, arrives("Orlo")));

        assertEquals(Set.of("elena", "marc"), scenario.activityAt(1).inSceneIds());
        assertEquals(Set.of("elena", "marc", "orlo"), scenario.activityAt(2).inSceneIds());
        assertEquals(Set.of("orlo"), scenario.activityAt(0).outsideIds());
    }

    @Test
    void withoutARosterEveryoneIsThereFromTheStart() {
        ChatScenario scenario = scenario(StartingRoster.EVERYBODY, act(1, List.of()));

        assertTrue(scenario.activityAt(1).outsideIds().isEmpty());
    }

    @Test
    void aRosterNamingNobodyKnownFailsLoudly() {
        var error = assertThrows(IllegalArgumentException.class, () -> scenario(new StartingRoster("Zorg"), act(1, List.of())));

        assertTrue(error.getMessage().contains("{npc-only Zorg}"), error.getMessage());
    }

    @Test
    void anActLeavingNobodyInTheStoryFailsLoudly() {
        assertThrows(IllegalArgumentException.class, () -> scenario(StartingRoster.EVERYBODY,
            act(1, List.of(ActivityDirective.leaves("Elena"), ActivityDirective.leaves("Marc"), ActivityDirective.leaves("Orlo")))));
    }
}
