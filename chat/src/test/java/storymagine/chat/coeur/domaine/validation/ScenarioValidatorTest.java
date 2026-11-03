package storymagine.chat.coeur.domaine.validation;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.pick.PickLibrary;
import storymagine.chat.coeur.domaine.pick.PickParagraphs;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.CharacterDelta;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.NpcOpening;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.scenario.StoryDirective;
import storymagine.chat.coeur.domaine.scenario.StoryTag;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioValidatorTest {

    private static final Cast CAST = new Cast(List.of(
        new Npc("elena", "Elena Voss", "Une marchande.", ""),
        new Npc("marcus", "Marcus", "Un soldat.", "")));

    private static final PickLibrary LIBRARY = tag -> {
        String content = Map.of("lieu", "Port.\n---\nMarche.\n---\nVille.", "mood", "Sombre.").get(tag.name());
        if (content == null) throw new IllegalStateException("{pick ... " + tag + "} : fichier introuvable");
        return PickParagraphs.parse(content);
    };

    private static ScenarioReport check(String text) {
        return ScenarioValidator.checkSources(List.of(new ScenarioSource("scenario.txt", text)), CAST, LIBRARY);
    }

    @Test
    void aCorrectScenarioHasNoIssue() {
        ScenarioReport report = check("Monde. {pick random lieu}\n{npc-starts Elena}\n[Alex entre.]\n"
            + "# Un\n{npc-delta secret Marcus : Il ment.}\nQuand écrire [NEXT ACT] : à la porte.\n");

        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test
    void aRepeatBlockWithItsZonesIsNotReportedAsUnknownCommands() {
        ScenarioReport report = check("Monde.\n{repeat order lieu}\n# Etape {n}\nLieu : {item}\n{end-repeat}\n");

        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test
    void repeatProblemsNameTheFileAndTheLineInsideIt() {
        ScenarioReport report = check("Monde.\n{repeat order lieu}\nLieu : {item2}\n{end-repeat}\n{n}\n");

        // The first problem of the file stops the unfolding : one error, pointing at the line.
        assertEquals(1, report.errors().size());
        assertEquals("scenario.txt", report.errors().get(0).where());
        assertTrue(report.errors().get(0).message().startsWith("ligne 2"), report.errors().get(0).message());
        assertTrue(check("{repeat random 9 lieu}\n{item}\n{end-repeat}").hasErrors());
        assertTrue(check("{repeat order inconnu}\n{item}\n{end-repeat}").hasErrors());
    }

    @Test
    void everyProblemIsListedWithItsFileAndLine() {
        ScenarioReport report = check("Monde.\n[PICK: lieu]\n{pick lieu}\n{pick random inconnu}\n{pick 9 lieu}\n"
            + "{npc-starts Zorglub}\nTexte {npc-starts} au milieu\n{npc-delta Elena : sans visibilite}\n"
            + "{npc-delta secret Zorglub : x}\n{teleport elena}\n");

        List<String> where = report.errors().stream().map(ScenarioIssue::where).toList();
        assertEquals(List.of("scenario.txt:2", "scenario.txt:3", "scenario.txt:4", "scenario.txt:5", "scenario.txt:6",
            "scenario.txt:7", "scenario.txt:8", "scenario.txt:9", "scenario.txt:10"), where);
        assertTrue(report.hasErrors());
    }

    @Test
    void uniqueAskingForMoreThanTheLibraryHoldsIsAnErrorEvenWhenEachCommandIsFine() {
        ScenarioReport report = check("{pick unique mood} puis {pick unique mood}");

        assertEquals(1, report.errors().size());
        assertTrue(report.errors().get(0).message().contains("mood"), report.errors().get(0).message());
    }

    @Test
    void anActWithoutNextActConditionAndAnUnnamedCharacterAreOnlyWarnings() {
        ChatScenario scenario = new ChatScenario("test", CAST, "Elena attend sur le quai.", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Point de départ : le quai."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Dernier acte.")));

        ScenarioReport report = ScenarioValidator.checkScenario(scenario);

        assertFalse(report.hasErrors());
        List<String> messages = report.warnings().stream().map(ScenarioIssue::message).toList();
        assertTrue(messages.stream().anyMatch(m -> m.contains("[NEXT ACT]")), messages.toString());
        assertTrue(messages.stream().anyMatch(m -> m.contains("Marcus")), messages.toString());
        assertTrue(messages.stream().noneMatch(m -> m.contains("Elena")), "Elena est citee dans la premisse");
    }

    @Test
    void aStyleWithACommandOrAnOldCommandIsAnError() {
        assertTrue(ScenarioValidator.checkStyleText("Phrases courtes.").issues().isEmpty());
        assertTrue(ScenarioValidator.checkStyleText("Ton : {pick random ton}").hasErrors());
        assertTrue(ScenarioValidator.checkStyleText("[NPC STARTS]").hasErrors());
    }

    @Test
    void aChanceMarkerIsAcceptedAnywhereInAStyleLineButMustBeWellFormed() {
        assertTrue(ScenarioValidator.checkStyleText("- {20%} Un detail sensoriel.").issues().isEmpty());
        assertTrue(ScenarioValidator.checkStyleText("Un detail sensoriel. {20%}").issues().isEmpty());
        assertTrue(ScenarioValidator.checkStyleText("{0%} Jamais.").hasErrors(), "0 % : ligne inutile");
        assertTrue(ScenarioValidator.checkStyleText("{150%} Trop.").hasErrors());
        assertTrue(ScenarioValidator.checkStyleText("{20%} Un {30%} detail.").hasErrors(), "un seul marqueur par ligne");
        assertTrue(ScenarioValidator.checkStyleText("- {20%}").hasErrors(), "rien a garder ou ecarter");
        assertTrue(ScenarioValidator.checkStyleText("{20} Un detail.").hasErrors(), "il manque le %");
    }

    @Test
    void aVeryLongStyleIsOnlyAWarning() {
        String longStyle = "x".repeat(ScenarioValidator.STYLE_WARNING_LENGTH + 1);
        ChatScenario scenario = new ChatScenario("test", CAST, "Elena et Marcus.", List.of(), null,
            storymagine.chat.coeur.domaine.scenario.NpcOpening.NONE, storymagine.chat.coeur.domaine.scenario.ScenarioLanguage.UNSPECIFIED,
            storymagine.chat.coeur.domaine.scenario.ScenarioStyle.of(longStyle));

        ScenarioReport report = ScenarioValidator.checkScenario(scenario);

        assertFalse(report.hasErrors());
        assertEquals(1, report.warnings().size(), report.issues().toString());
        assertEquals("style.txt", report.warnings().get(0).where());
    }

    @Test
    void theLastActNeedsNoNextActCondition() {
        ChatScenario scenario = new ChatScenario("test", CAST, "Elena et Marcus.", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Quand écrire [NEXT ACT] : plus tard."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Fin.")));

        assertTrue(ScenarioValidator.checkScenario(scenario).issues().isEmpty());
    }

    @Test
    void npcModeAndNpcDoAreValidatedLikeTheOtherNpcCommands() {
        assertTrue(check("# Un\n{npc-mode do on Elena}\n{npc-mode do off *}\n{npc-do Marcus}\n").issues().isEmpty());

        ScenarioReport report = check("# Un\n{npc-mode do on Zorglub}\n{npc-mode fly on Elena}\n{npc-do Zorglub}\nEn {npc-do} milieu\n");

        List<String> where = report.errors().stream().map(ScenarioIssue::where).toList();
        assertEquals(List.of("scenario.txt:2", "scenario.txt:3", "scenario.txt:4", "scenario.txt:5"), where, report.issues().toString());
    }

    private static final Cast CAST_WITH_TAGS = new Cast(List.of(
        new Npc("lea", "Léa Martin", "Réservée.", "Elle cache quelque chose.", List.of(
            new StoryTag("froide", CharacterDelta.Visibility.PUBLIC, "Distante avec Marc."),
            new StoryTag("chatons", CharacterDelta.Visibility.SECRET, "Touchée par Marc.")))));

    private static ScenarioReport checkTags(String text) {
        return ScenarioValidator.checkSources(List.of(new ScenarioSource("scenario.txt", text)), CAST_WITH_TAGS, LIBRARY);
    }

    @Test
    void npcStoryIsValidatedAgainstTheCharactersAndTheirTags() {
        assertTrue(checkTags("# Un\n{npc-story on Léa froide}\n{npc-story off Martin froide}\n").issues().isEmpty());

        ScenarioReport report = checkTags("# Un\n{npc-story on Zorglub froide}\n{npc-story on Léa inconnu}\n"
            + "{npc-story on Léa SECRET}\n{npc-story on Léa}\nEn {npc-story on Léa froide} milieu\n");

        List<String> where = report.errors().stream().map(ScenarioIssue::where).toList();
        assertEquals(List.of("scenario.txt:2", "scenario.txt:3", "scenario.txt:4", "scenario.txt:5", "scenario.txt:6"), where, report.issues().toString());
    }

    @Test
    void castIsCheckedLineByLineAndItsVariablesMustBeDeclaredAbove() {
        assertTrue(check("# Un\n{cast A (elena)}\n{cast B}\n{npc-starts %A%}\nTexte avec %A% et %B%.\n").issues().isEmpty());

        ScenarioReport report = check("# Un\n%A% entre.\n{cast A (zorglub)}\n{cast B libre}\n{cast C}\nEn {cast D} milieu\n");

        List<String> where = report.errors().stream().map(ScenarioIssue::where).sorted().toList();
        assertEquals(List.of("scenario.txt:2", "scenario.txt:3", "scenario.txt:4", "scenario.txt:6"), where, report.issues().toString());
    }

    @Test
    void aVariableOfARepeatBlockIsForgottenAfterTheBlock() {
        assertTrue(check("# Un\n{repeat order cast H}\n%H%\n{end-repeat}\n").issues().isEmpty());

        ScenarioReport report = check("# Un\n{repeat order cast H}\n%H%\n{end-repeat}\n%H%\n");

        assertEquals(List.of("scenario.txt:5"), report.errors().stream().map(ScenarioIssue::where).toList(), report.issues().toString());
    }

    @Test
    void anImpossibleCastCombinationIsAnErrorNamingTheCommand() {
        ScenarioReport report = check("# Un\n{cast A (elena)}\n{cast B (elena)}\n");

        assertEquals(1, report.errors().size(), report.issues().toString());
        assertTrue(report.errors().get(0).message().contains("{cast B (elena)}"), report.errors().get(0).message());
    }

    @Test
    void theCommandsOfARepeatOverTheCastAreCheckedToo() {
        assertTrue(check("# Un\n{repeat 2}\nEtape {n}\n{end-repeat}\n{repeat random 2 cast H (elena, marcus)}\n%H%\n{end-repeat}\n").issues().isEmpty());

        ScenarioReport report = check("# Un\n{repeat order 3 cast H (elena, marcus)}\n%H%\n{end-repeat}\n");

        assertEquals(1, report.errors().size(), report.issues().toString());
        assertTrue(report.errors().get(0).message().contains("2 personnage"), report.errors().get(0).message());
    }

    @Test
    void theOldDeltaNameIsRefusedAndTheNewNameIsGiven() {
        ScenarioReport report = check("# Un\n{delta-pnj public Elena : Elle boite.}\n");

        assertEquals(1, report.errors().size(), report.issues().toString());
        assertTrue(report.errors().get(0).message().contains("npc-delta"), report.errors().get(0).message());
    }

    @Test
    void aTagNeverSwitchedOnAndAnOffOnATagThatIsNotOnAreWarnings() {
        ScenarioAct one = new ScenarioAct(ActNumber.of(1), "Un", "Texte. Quand écrire [NEXT ACT] : fin.", List.of(), NpcOpening.NONE,
            List.of(), List.of(), List.of(new StoryDirective(false, "Léa", "froide"), new StoryDirective(true, "Léa", "froide")));
        ScenarioAct two = new ScenarioAct(ActNumber.of(2), "Deux", "Texte.", List.of(), NpcOpening.NONE,
            List.of(), List.of(), List.of(new StoryDirective(false, "Léa", "froide")));
        ChatScenario scenario = new ChatScenario("t", CAST_WITH_TAGS, "Léa est là.", List.of(one, two));

        List<String> warnings = ScenarioValidator.checkScenario(scenario).issues().stream()
            .map(ScenarioIssue::display).filter(m -> m.contains("tag")).toList();

        assertEquals(2, warnings.size(), warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("acte 1") && w.contains("off")), warnings.toString());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("chatons") && w.contains("jamais")), warnings.toString());
    }
}
