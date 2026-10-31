package storymagine.chat.coeur.domaine.validation;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.pick.PickLibrary;
import storymagine.chat.coeur.domaine.pick.PickParagraphs;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

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
            + "# Un\n{delta-pnj secret Marcus : Il ment.}\nQuand écrire [NEXT ACT] : à la porte.\n");

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
            + "{npc-starts Zorglub}\nTexte {npc-starts} au milieu\n{delta-pnj Elena : sans visibilite}\n"
            + "{delta-pnj secret Zorglub : x}\n{teleport elena}\n");

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
}
