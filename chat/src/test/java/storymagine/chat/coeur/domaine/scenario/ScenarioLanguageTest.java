package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioLanguageTest {

    @Test
    void frenchAndEnglishNamesIsoCodesAndAccentsAllGiveTheEnglishName() {
        assertEquals("French", ScenarioLanguage.parse("français").englishName());
        assertEquals("French", ScenarioLanguage.parse("FRANCAIS").englishName());
        assertEquals("French", ScenarioLanguage.parse("fr").englishName());
        assertEquals("English", ScenarioLanguage.parse(" Anglais ").englishName());
        assertEquals("Spanish", ScenarioLanguage.parse("es").englishName());
    }

    @Test
    void anUnknownLanguageIsKeptVerbatim() {
        assertEquals("Latin", ScenarioLanguage.parse("Latin").englishName());
    }

    @Test
    void blankOrNullIsUnspecified() {
        assertFalse(ScenarioLanguage.parse("  ").isSpecified());
        assertFalse(ScenarioLanguage.parse(null).isSpecified());
        assertTrue(ScenarioLanguage.parse("fr").isSpecified());
    }
}
