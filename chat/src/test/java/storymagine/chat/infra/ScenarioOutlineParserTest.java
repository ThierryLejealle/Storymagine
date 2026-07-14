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
}
