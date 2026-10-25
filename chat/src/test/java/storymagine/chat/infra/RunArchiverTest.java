package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunArchiverTest {

    private static void writeRun(Path current) throws IOException {
        Files.createDirectories(current.resolve("turns"));
        Files.writeString(current.resolve("turns/000001.md"), "one", StandardCharsets.UTF_8);
        Files.writeString(current.resolve("turns/000002.md"), "two", StandardCharsets.UTF_8);
        Files.writeString(current.resolve("turns/000002-compaction-f000001.md"), "summary", StandardCharsets.UTF_8);
        Files.writeString(current.resolve("state.txt"), "act=1\n", StandardCharsets.UTF_8);
    }

    @Test
    void zipThenExtractRestoresEveryFileOfTheRun(@TempDir Path dir) throws IOException {
        Path current = dir.resolve("current");
        writeRun(current);
        Path zip = dir.resolve("out/run.zip");

        RunArchiver.zip(current, zip);
        Path restored = dir.resolve("restored");
        RunArchiver.extractInto(zip, restored);

        assertEquals("one", Files.readString(restored.resolve("turns/000001.md")));
        assertEquals("summary", Files.readString(restored.resolve("turns/000002-compaction-f000001.md")));
        assertEquals("act=1\n", Files.readString(restored.resolve("state.txt")));
    }

    @Test
    void extractReplacesWhatWasInTheTargetDirectory(@TempDir Path dir) throws IOException {
        Path current = dir.resolve("current");
        writeRun(current);
        Path zip = dir.resolve("run.zip");
        RunArchiver.zip(current, zip);
        Files.writeString(current.resolve("turns/000003.md"), "added after the zip", StandardCharsets.UTF_8);

        RunArchiver.extractInto(zip, current);

        assertFalse(Files.exists(current.resolve("turns/000003.md")), "extraire remplace current/, il ne fusionne pas");
    }

    @Test
    void zipLeavesOutAStrayTemporaryFile(@TempDir Path dir) throws IOException {
        Path current = dir.resolve("current");
        writeRun(current);
        Files.writeString(current.resolve("turns/000003.md.tmp"), "half written", StandardCharsets.UTF_8);
        Path zip = dir.resolve("run.zip");

        RunArchiver.zip(current, zip);
        Path restored = dir.resolve("restored");
        RunArchiver.extractInto(zip, restored);

        assertFalse(Files.exists(restored.resolve("turns/000003.md.tmp")));
    }

    @Test
    void lastTurnNumberReadsTheEntryNamesAndIgnoresCompactions(@TempDir Path dir) throws IOException {
        Path current = dir.resolve("current");
        writeRun(current);
        Files.writeString(current.resolve("turns/000007-compaction-f000005.md"), "later summary", StandardCharsets.UTF_8);
        Path zip = dir.resolve("run.zip");
        RunArchiver.zip(current, zip);

        assertEquals(2, RunArchiver.lastTurnNumber(zip));
    }

    @Test
    void zippingARunThatDoesNotExistGivesAnEmptyZip(@TempDir Path dir) {
        Path zip = dir.resolve("empty.zip");

        RunArchiver.zip(dir.resolve("no-such-run"), zip);

        assertTrue(Files.isRegularFile(zip));
        assertEquals(0, RunArchiver.lastTurnNumber(zip));
    }

    @Test
    void extractRefusesAZipWithAnEntryThatCouldEscapeTheRun(@TempDir Path dir) throws IOException {
        Path zip = dir.resolve("evil.zip");
        try (var out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("turns/000001.md"));
            out.write("fine".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new ZipEntry("../evil.txt"));
            out.write("bad".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        Path target = dir.resolve("current");

        assertThrows(IllegalStateException.class, () -> RunArchiver.extractInto(zip, target));
        assertFalse(Files.exists(dir.resolve("evil.txt")));
        assertFalse(Files.exists(target.resolve("turns/000001.md")), "rien n'est ecrit tant que tout le zip n'est pas valide");
    }
}
