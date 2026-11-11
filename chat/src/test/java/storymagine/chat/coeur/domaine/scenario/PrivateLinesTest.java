package storymagine.chat.coeur.domaine.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "{private Name}" texts : who reads what, and what the author may not write inside. */
class PrivateLinesTest {

    private static final Npc FRANCK = new Npc("franck", "Franck", "Un joueur.", "");
    private static final Npc ALEX = new Npc("alex", "Alex", "Un ami.", "");
    private static final Npc MINA = new Npc("mina", "Mina", "Une voisine.", "");
    private static final Cast CAST = new Cast(List.of(FRANCK, ALEX, MINA));

    private static final String SCENE = """
        Ils sortent leurs consoles.
        {private Franck} Franck veut gagner et va tricher.
        {private Alex}
        Alex veut s'amuser.
        Il ne se doute de rien.
        {end-private}
        Le salon est calme.""";

    @Test
    void aLineAddressedToOneCharacterIsReadOnlyByThatCharacter() {
        String franck = PrivateLines.visibleTo(SCENE, CAST, FRANCK);

        assertTrue(franck.contains("Franck veut gagner et va tricher."), franck);
        assertFalse(franck.contains("Alex veut"), franck);
        assertFalse(PrivateLines.visibleTo(SCENE, CAST, ALEX).contains("tricher"));
    }

    @Test
    void aBlockIsReadOnlyByItsCharacterAndItsLinesStayInOrder() {
        String alex = PrivateLines.visibleTo(SCENE, CAST, ALEX);

        assertEquals("""
            Ils sortent leurs consoles.
            Alex veut s'amuser.
            Il ne se doute de rien.
            Le salon est calme.""", alex);
    }

    @Test
    void whatIsNotPrivateIsReadByEverybodyAndTheCommandsNeverAppear() {
        String mina = PrivateLines.visibleTo(SCENE, CAST, MINA);

        assertEquals("Ils sortent leurs consoles.\nLe salon est calme.", mina);
        for (Npc reader : CAST.npcs()) assertFalse(PrivateLines.visibleTo(SCENE, CAST, reader).contains("{"));
    }

    @Test
    void severalNamesShareTheSamePrivateText() {
        String text = "{private Franck, Mina} Un secret a deux.\nPublic.";

        assertTrue(PrivateLines.visibleTo(text, CAST, FRANCK).contains("Un secret a deux."));
        assertTrue(PrivateLines.visibleTo(text, CAST, MINA).contains("Un secret a deux."));
        assertFalse(PrivateLines.visibleTo(text, CAST, ALEX).contains("secret"));
    }

    @Test
    void aTextWithoutCommandComesBackUnchanged() {
        assertEquals("Rien de prive.\n\nIci.", PrivateLines.visibleTo("Rien de prive.\n\nIci.", CAST, ALEX));
    }

    @Test
    void anUnknownNameIsRefusedWithItsLine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("Texte.\n{private Zorg} Il sait.", CAST));

        assertTrue(e.getMessage().contains("ligne 2") && e.getMessage().contains("Zorg"), e.getMessage());
    }

    @Test
    void anUnclosedBlockAStrayEndAndANestedBlockAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> PrivateLines.validate("{private Alex}\nSecret.", CAST));
        assertThrows(IllegalArgumentException.class, () -> PrivateLines.validate("Texte.\n{end-private}", CAST));
        assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("{private Alex}\n{private Franck}\nX.\n{end-private}\n{end-private}", CAST));
    }

    @Test
    void aBlockMustBeClosedBeforeTheNextHeading() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("{private Alex}\nSecret.\n# Suite\n{end-private}", CAST));

        assertTrue(e.getMessage().contains("avant le titre"), e.getMessage());
    }

    @Test
    void aCommandInTheMiddleOfALineIsRefusedNotSilentlyKept() {
        assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("Il dit {private Alex} un secret.", CAST));
    }

    @Test
    void theNextActConditionAndAPlayerLineCannotBePrivate() {
        assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("{private Alex} Quand ecrire [NEXT ACT] : a la porte.", CAST));
        assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("{private Alex}\n[Alex entre.]\n{end-private}", CAST));
        assertThrows(IllegalArgumentException.class,
            () -> PrivateLines.validate("{private Alex} [Alex entre.]", CAST));
    }

    @Test
    void aNameHoldingAPercentIsALaterDrawAndIsNotCheckedYet() {
        PrivateLines.validate("{private %HERO%} Il sait.", CAST);
    }
}
