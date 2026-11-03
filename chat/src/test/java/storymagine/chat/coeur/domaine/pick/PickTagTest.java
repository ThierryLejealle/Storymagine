package storymagine.chat.coeur.domaine.pick;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickTagTest {

    @Test
    void aScenarioTagPointsIntoPicksAndASharedTagIntoLib() {
        PickTag own = new PickTag("lieu");
        PickTag shared = new PickTag("/lib/lieux/ville");

        assertFalse(own.isShared());
        assertEquals("picks/lieu.txt", own.file());
        assertTrue(shared.isShared());
        assertEquals("lieux/ville", shared.sharedPath());
        assertEquals("lib/lieux/ville.txt", shared.file());
    }

    @Test
    void aTagCanNeverPointOutsidePicksOrLib() {
        for (String bad : new String[]{"/lib/../secret", "/lib/", "/lib", "lib/x", "/lib/a.b", "/lib//x", "/etc/passwd", "../x", "a/b", "", "/lib/a b"}) {
            assertThrows(IllegalArgumentException.class, () -> new PickTag(bad), bad);
        }
    }
}
