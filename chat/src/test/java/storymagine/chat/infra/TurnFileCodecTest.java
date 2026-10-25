package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.NumberedTurn;
import storymagine.chat.coeur.domaine.session.TurnNumber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnFileCodecTest {

    @Test
    void formatPutsTheNumberAndActOnTheFirstLineThenTheSpeakerHeaderThenTheTextVerbatim() {
        NumberedTurn turn = new NumberedTurn(TurnNumber.of(13), 2,
            new ChatTurn(ChatTurn.Speaker.LLM, "*smiles*\n\nWelcome.", "", "elena"));

        assertEquals("----- #13 · acte 2 -----\n### LLM: elena\n*smiles*\n\nWelcome.\n", TurnFileCodec.format(turn));
    }

    @Test
    void roundTripsEverySpeakerKind() {
        for (ChatTurn turn : new ChatTurn[] {
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hello."),
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hi.", "", "elena"),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hi.") // LLM sans npcId (ancienne partie mono-PNJ) : en-tete "### LLM" nu
        }) {
            NumberedTurn original = new NumberedTurn(TurnNumber.of(7), 0, turn);

            assertEquals(original, TurnFileCodec.parse(TurnFileCodec.format(original), "test"));
        }
    }

    @Test
    void aTurnWhoseTextLooksLikeASeparatorLineStillRoundTrips() {
        // le separateur n'est cherche qu'en toute premiere ligne : un texte qui en contient un ne casse rien
        NumberedTurn original = new NumberedTurn(TurnNumber.of(3), 1,
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Look:\n----- #99 · acte 7 -----\n### PLAYER\nTricky."));

        assertEquals(original, TurnFileCodec.parse(TurnFileCodec.format(original), "test"));
    }

    @Test
    void parseToleratesWindowsLineEndings() {
        NumberedTurn parsed = TurnFileCodec.parse("----- #2 · acte 0 -----\r\n### PLAYER\r\nHi.\r\n", "test");

        assertEquals("Hi.", parsed.turn().text());
        assertEquals(TurnNumber.of(2), parsed.number());
    }

    @Test
    void parseRejectsAFileWithoutTheSeparatorLine() {
        assertThrows(IllegalStateException.class, () -> TurnFileCodec.parse("### PLAYER\nHi.", "test"));
    }

    @Test
    void parseRejectsAnUnknownSpeakerHeader() {
        assertThrows(IllegalStateException.class,
            () -> TurnFileCodec.parse("----- #1 · acte 0 -----\n### ROBOT\nBeep.", "test"));
    }
}
