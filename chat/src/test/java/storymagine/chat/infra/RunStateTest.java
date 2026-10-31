package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.activity.ForcedActivity;
import storymagine.chat.coeur.domaine.activity.NpcActivity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RunStateTest {

    @Test
    void forcedLevelsAndTheirAnchorsRoundTrip() {
        Map<String, ForcedActivity> forced = new LinkedHashMap<>();
        forced.put("elena", new ForcedActivity(NpcActivity.ACTIVE, "1.2"));
        forced.put("marcus", new ForcedActivity(NpcActivity.ABSENT, ""));

        RunState parsed = RunState.parse(new RunState(3, null, null, forced).format());

        assertEquals(3, parsed.act());
        assertEquals(forced, parsed.forced());
    }

    @Test
    void nothingForcedWritesNoForcedKey() {
        String text = new RunState(2, null, null, Map.of()).format();

        assertEquals("act=2\n", text);
        assertNull(RunState.parse(text).forced());
    }

    @Test
    void theKeysOfTheOldFormatAreStillRead() {
        RunState parsed = RunState.parse("act=1\npresent=elena,marcus\ninterjecting=elena\n");

        assertEquals(Set.of("elena", "marcus"), parsed.present());
        assertEquals(Set.of("elena"), parsed.interjecting());
        assertNull(parsed.forced());
    }

    @Test
    void anUnknownLevelInTheFileIsIgnoredNotFatal() {
        RunState parsed = RunState.parse("forced=elena:flying@;marcus:passive@1\n");

        assertEquals(Map.of("marcus", new ForcedActivity(NpcActivity.PASSIVE, "1")), parsed.forced());
    }
}
