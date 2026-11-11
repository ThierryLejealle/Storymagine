package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioOutlineParserTest {

    @Test
    void noHeadingAtAllMeansWholeFileIsThePremiseWithNoActs() {
        var outline = ScenarioOutlineParser.parse("Just a plain premise, no acts here.");

        assertEquals("Just a plain premise, no acts here.", outline.premise());
        assertTrue(outline.acts().isEmpty());
    }

    @Test
    void scenarioMarkerDelimitsThePremiseFromTheFirstAct() {
        var outline = ScenarioOutlineParser.parse("""
            #SCENARIO
            The premise text.

            # Act One
            Act one body.""");

        assertEquals("The premise text.", outline.premise());
        assertEquals(1, outline.acts().size());
        assertEquals("Act One", outline.acts().get(0).title());
    }

    @Test
    void flatActsGetSequentialTopLevelNumbers() {
        var outline = ScenarioOutlineParser.parse("""
            #SCENARIO
            Premise.

            # First
            Body one.

            # Second
            Body two.""");

        assertEquals(ActNumber.of(1), outline.acts().get(0).number());
        assertEquals(ActNumber.of(2), outline.acts().get(1).number());
    }

    @Test
    void onlyLeavesAreEmittedAndTheirTextInheritsEveryAncestorsOwnBody() {
        var outline = ScenarioOutlineParser.parse("""
            #SCENARIO
            Premise.

            # Le Début
            Texte propre de l'acte 1.

            ## Le début du Début
            Texte propre de 1.1.

            ## Suite du début
            Texte propre de 1.2.""");

        // "1" lui-meme n'est jamais emis (il a des enfants) : seules 1.1 et 1.2 sont "courants".
        assertEquals(2, outline.acts().size());

        ScenarioAct leaf1 = outline.acts().get(0);
        assertEquals(ActNumber.of(1, 1), leaf1.number());
        assertEquals("Le début du Début", leaf1.title());
        assertEquals("Texte propre de l'acte 1.\n\nTexte propre de 1.1.", leaf1.text());

        ScenarioAct leaf2 = outline.acts().get(1);
        assertEquals(ActNumber.of(1, 2), leaf2.number());
        assertEquals("Texte propre de l'acte 1.\n\nTexte propre de 1.2.", leaf2.text());
    }

    @Test
    void ancestorBeatFiresOnlyOnceWhenFirstEnteringItsBranch() {
        var outline = ScenarioOutlineParser.parse("""
            #SCENARIO
            Premise.

            # Le Début
            [Beat de l'acte 1.]
            Texte propre de l'acte 1.

            ## Le début du Début
            [Beat de 1.1.]
            Texte propre de 1.1.

            ## Suite du début
            [Beat de 1.2.]
            Texte propre de 1.2.""");

        ScenarioAct leaf1 = outline.acts().get(0); // 1.1 : premiere feuille jamais visitee -> ancetre "1" inclus
        assertEquals(java.util.List.of("Beat de l'acte 1.", "Beat de 1.1."), leaf1.beats());

        ScenarioAct leaf2 = outline.acts().get(1); // 1.2 : "1" deja actif -> seul son propre beat
        assertEquals(java.util.List.of("Beat de 1.2."), leaf2.beats());
    }

    @Test
    void windowsCrlfLineEndingsDoNotHideTheHeadingsFromTheParser() {
        // Bug reel observe : un scenario.txt enregistre avec des fins de ligne Windows (CRLF)
        // perdait TOUS ses titres "#"/"##" — HEADING.matches() echouait sur chaque ligne a cause du
        // '\r' final invisible, scenario.acts() se retrouvait vide, et tout le fichier retombait en
        // premisse plate (voir evols/2026-07-17-...). Meme contenu que
        // scenarioMarkerDelimitsThePremiseFromTheFirstAct, juste avec \r\n au lieu de \n.
        String withCrlf = "#SCENARIO\r\nThe premise text.\r\n\r\n# Act One\r\nAct one body.";

        var outline = ScenarioOutlineParser.parse(withCrlf);

        assertEquals("The premise text.", outline.premise());
        assertEquals(1, outline.acts().size());
        assertEquals("Act One", outline.acts().get(0).title());
    }

    @Test
    void nextActConditionLineStaysInsideTheResolvedTextVerbatim() {
        var outline = ScenarioOutlineParser.parse("""
            #SCENARIO
            Premise.

            # First
            Body one.

            Quand écrire [NEXT ACT] : quand telle condition est remplie.

            # Second
            Body two.""");

        assertTrue(outline.acts().get(0).text().contains("Quand écrire [NEXT ACT]"));
    }

    // ── {npc-only} : the starting roster, before the first [NEXT ACT] ──────

    @Test
    void npcOnlyInThePremiseSetsTheStartingRosterAndIsCutOutOfTheText() {
        var outline = ScenarioOutlineParser.parse("""
            #SCENARIO
            The premise.
            {npc-only Elena, Marc}

            # One
            Body.""");

        assertEquals("Elena, Marc", outline.startingRoster().namesText());
        assertEquals("The premise.", outline.premise());
    }

    @Test
    void npcOnlyInTheFirstSceneCountsToo() {
        var outline = ScenarioOutlineParser.parse("""
            Premise.

            # One
            {npc-only Elena}
            Body one.

            ## One point one
            Deeper.

            # Two
            Body two.""");

        assertEquals("Elena", outline.startingRoster().namesText());
        assertEquals("Body one.", outline.acts().get(0).text().lines().findFirst().orElse(""));
    }

    @Test
    void noNpcOnlyMeansEverybodyIsThere() {
        assertTrue(ScenarioOutlineParser.parse("Premise.\n\n# One\nBody.").startingRoster().isEverybody());
    }

    @Test
    void npcOnlyAfterTheFirstSceneIsAnErrorNotASilentClassicMode() {
        var error = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            ScenarioOutlineParser.parse("""
                Premise.

                # One
                Body one.

                # Two
                {npc-only Elena}
                Body two."""));

        assertTrue(error.getMessage().contains("premier [NEXT ACT]"), error.getMessage());
        assertTrue(error.getMessage().contains("{npc-arrives}"), error.getMessage());
    }

    @Test
    void twoNpcOnlyAreAnError() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
            ScenarioOutlineParser.parse("""
                Premise.
                {npc-only Elena}

                # One
                {npc-only Marc}
                Body."""));
    }

    @Test
    void anArrivesUnderAnActIsKeptInTheActivityPathOfItsActs() {
        var outline = ScenarioOutlineParser.parse("""
            Premise.

            # One
            Body one.

            # Two
            {npc-arrives goblin Grik, Snak}
            Body two.""");

        var path = outline.acts().get(1).activityPath();
        assertEquals("goblin Grik, Snak", path.get(path.size() - 1).get(0).namesText());
        assertEquals("Body two.", outline.acts().get(1).text());
    }
}
