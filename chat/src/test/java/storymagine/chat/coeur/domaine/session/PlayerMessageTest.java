package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerMessageTest {

    @Test
    void plainDialogueStaysDialogueMode() {
        PlayerMessage m = PlayerMessage.parse("*enters the room* Hello there.");
        assertEquals(PlayerMessage.Mode.DIALOGUE, m.mode());
        assertEquals("*enters the room* Hello there.", m.text());
    }

    @Test
    void doPrefixSwitchesToNarratorHandoff() {
        PlayerMessage m = PlayerMessage.parse("DO: the guard finally notices them.");
        assertEquals(PlayerMessage.Mode.NARRATOR_HANDOFF, m.mode());
        assertEquals("the guard finally notices them.", m.text());
    }

    @Test
    void doPrefixIsCaseInsensitiveAndTrimsWhitespace() {
        PlayerMessage m = PlayerMessage.parse("  do:   go on.  ");
        assertEquals(PlayerMessage.Mode.NARRATOR_HANDOFF, m.mode());
        assertEquals("go on.", m.text());
    }

    @Test
    void doOnlyMattersAsALeadingPrefixNotMidText() {
        PlayerMessage m = PlayerMessage.parse("Please DO: something for me.");
        assertEquals(PlayerMessage.Mode.DIALOGUE, m.mode());
        assertEquals("Please DO: something for me.", m.text());
    }

    @Test
    void oocLineIsOrdinaryDialogueMode() {
        // OOC: n'a pas de traitement mecanique special cote parsing — c'est une convention lue
        // par le LLM via ChatPromptBuilder.RULES, pas un Mode distinct.
        PlayerMessage m = PlayerMessage.parse("OOC: can we skip ahead to the next morning?");
        assertEquals(PlayerMessage.Mode.DIALOGUE, m.mode());
        assertEquals("OOC: can we skip ahead to the next morning?", m.text());
    }
}
