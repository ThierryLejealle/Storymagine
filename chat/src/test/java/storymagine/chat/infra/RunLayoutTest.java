package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.infra.RunLayout.SequenceFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunLayoutTest {

    @Test
    void namesAreZeroPaddedToSixDigits() {
        assertEquals("000013.md", RunLayout.turnFileName(13));
        assertEquals("000013-compaction-f000012.md", RunLayout.compactionFileName(13, 12));
    }

    @Test
    void parseReadsTheNumbersBackFromTheNames() {
        SequenceFile turn = RunLayout.parse(Path.of("000013.md")).orElseThrow();
        assertEquals(SequenceFile.Kind.TURN, turn.kind());
        assertEquals(13, turn.number());

        SequenceFile compaction = RunLayout.parse(Path.of("000013-compaction-f000012.md")).orElseThrow();
        assertEquals(SequenceFile.Kind.COMPACTION, compaction.kind());
        assertEquals(13, compaction.number());
        assertEquals(12, compaction.foldedUpTo());
    }

    @Test
    void parseIgnoresAnythingThatIsNotATurnOrACompactionFile() {
        assertTrue(RunLayout.parse(Path.of("notes.txt")).isEmpty());
        assertTrue(RunLayout.parse(Path.of("000013.md.tmp")).isEmpty());
        assertTrue(RunLayout.parse(Path.of("13.md")).isEmpty());
    }

    @Test
    void listSequenceOrdersByNumberWithACompactionRightAfterTheTurnItWasMadeAfter(@TempDir Path dir) throws IOException {
        // l'ordre alphabetique mettrait "000013-compaction..." AVANT "000013.md" ('-' < '.') : on trie sur les nombres
        for (String name : List.of("000014.md", "000013-compaction-f000012.md", "000013.md", "000012.md")) {
            Files.writeString(dir.resolve(name), "x");
        }

        List<String> ordered = RunLayout.listSequence(dir).stream().map(f -> f.path().getFileName().toString()).toList();

        assertEquals(List.of("000012.md", "000013.md", "000013-compaction-f000012.md", "000014.md"), ordered);
    }

    @Test
    void listSequenceSortsNumericallyPastSixDigits(@TempDir Path dir) throws IOException {
        for (String name : List.of("1000000.md", "999999.md")) Files.writeString(dir.resolve(name), "x");

        List<Integer> numbers = RunLayout.listSequence(dir).stream().map(SequenceFile::number).toList();

        assertEquals(List.of(999_999, 1_000_000), numbers);
    }

    @Test
    void listSequenceOfAMissingDirectoryIsEmpty(@TempDir Path dir) {
        assertTrue(RunLayout.listSequence(dir.resolve("nope")).isEmpty());
    }
}
