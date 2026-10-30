package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerThoughtsTest {

    @Test
    void removesThoughtsAndTidiesTheSpacingAroundThem() {
        assertEquals("*I nod slowly* \"I see.\"",
            PlayerThoughts.strip("(she's lying to me) *I nod slowly* \"I see.\""));
        assertEquals("Fine. Let's go.", PlayerThoughts.strip("Fine. (I don't trust him) Let's go."));
    }

    @Test
    void removesNestedThoughtsFromTheInsideOut() {
        assertEquals("Hello.", PlayerThoughts.strip("Hello. (I wonder (is she hiding something?) really)"));
    }

    @Test
    void blankWhenTheMessageIsOnlyThoughts() {
        assertEquals("", PlayerThoughts.strip("(she's lying) (and I know it)"));
    }

    @Test
    void leavesTextWithoutAClosedPairUntouched() {
        assertEquals("Fine :) see you tomorrow", PlayerThoughts.strip("Fine :) see you tomorrow"));
        assertEquals("Open ( never closed", PlayerThoughts.strip("Open ( never closed"));
    }
}
