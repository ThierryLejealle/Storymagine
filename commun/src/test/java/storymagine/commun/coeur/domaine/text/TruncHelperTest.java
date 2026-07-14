package storymagine.commun.coeur.domaine.text;

import org.junit.jupiter.api.Test;
import storymagine.commun.coeur.ports.LogPort;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TruncHelperTest {

    @Test
    void textReturnsUnchangedWhenUnderBudget() {
        String result = TruncHelper.create().text("Une phrase courte.", 100, "field");
        assertEquals("Une phrase courte.", result);
    }

    @Test
    void textCutsAtLastSentenceBoundaryBeforeLimit() {
        String result = TruncHelper.create().text("Phrase une. Phrase deux. Phrase trois.", 24, "field");
        assertEquals("Phrase une. Phrase deux.", result);
    }

    @Test
    void textHardCutsWithEllipsisWhenNoBoundaryFound() {
        String result = TruncHelper.create().text("Unmotunique", 5, "field");
        assertEquals("Unmot…", result);
    }

    @Test
    void tailTextKeepsEndStartingAtNextSentence() {
        String result = TruncHelper.create().tailText("Phrase une. Phrase deux. Phrase trois.", 15, "field");
        assertEquals("Phrase trois.", result);
    }

    @Test
    void listKeepsOnlyWholeLinesThatFit() {
        String result = TruncHelper.create().list("- item un\n- item deux\n- item trois", 21, "field");
        assertEquals("- item un\n- item deux", result);
    }

    @Test
    void listFallsBackToHeadCutWhenFirstLineAloneOverflows() {
        String result = TruncHelper.create().list("- unitemtroplongpourtenir", 9, "field");
        assertEquals("- unitemt…", result);
    }

    @Test
    void blockListKeepsOnlyWholeMultiLineBlocksThatFit() {
        String result = TruncHelper.create().blockList(
                "#### Alice\nDétails Alice\n\n#### Bob\nDétails Bob\n\n#### Carl\nDétails Carl",
                46, "field");
        assertEquals("#### Alice\nDétails Alice\n\n#### Bob\nDétails Bob", result);
    }

    @Test
    void blockListFallsBackToHeadCutWhenFirstBlockAloneOverflows() {
        String result = TruncHelper.create().blockList("#### Alicebientroplonguepourtenir", 10, "field");
        assertEquals("#### Alice…", result);
    }

    @Test
    void blankAndNullInputsReturnEmptyWithoutLogging() {
        TruncHelper t = TruncHelper.create();
        assertEquals("", t.text(null, 10, "a"));
        assertEquals("", t.text("   ", 10, "b"));
        assertEquals("", t.list(null, 10, "c"));

        List<String> logged = new ArrayList<>();
        t.logIfTruncated(warnCapturingLog(logged), "Agent");
        assertTrue(logged.isEmpty());
    }

    @Test
    void logIfTruncatedEmitsOneLineListingEveryCutField() {
        TruncHelper t = TruncHelper.create();
        t.text("Phrase une. Phrase deux. Phrase trois.", 24, "focusText");
        t.list("- item un\n- item deux\n- item trois", 21, "charactersText");
        t.text("Assez court", 100, "untouched");

        List<String> logged = new ArrayList<>();
        t.logIfTruncated(warnCapturingLog(logged), "ChapterPlanner");

        assertEquals(1, logged.size());
        assertEquals("ChapterPlanner: troncature — focusText, charactersText", logged.get(0));
    }

    private static LogPort warnCapturingLog(List<String> sink) {
        return new LogPort() {
            @Override public void phaseHeader(String label, String detail) {}
            @Override public void step(String name, long ms, String note) {}
            @Override public void critic(String name, double score, long ms, List<String> problems) {}
            @Override public void scoresSummary(double avg, double avgThreshold, double minScore, double eliminationThreshold, boolean passed, boolean forcedRetry, String retryHint) {}
            @Override public void llmCall(String agentLabel, long ms, int tokIn, int tokOut, double tokPerSec, Boolean think) {}
            @Override public void chapterPlan(String chapterTitle, String planText) {}
            @Override public void sequenceText(String chapterTitle, int seqIdx, String text) {}
            @Override public void sessionEnd() {}
            @Override public void warn(String message) { sink.add(message); }
        };
    }
}
