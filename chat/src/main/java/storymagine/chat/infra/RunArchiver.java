package storymagine.chat.infra;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Zips and unzips a run's current/ directory (turns/ and state.txt, entry names relative to
 * current/) — the one format shared by save points and archived runs. A zip is immutable once
 * written : it is built under a temporary name then moved into place. Extraction only accepts the
 * entry names a run can legitimately contain, so a doctored zip can never write outside current/.
 */
final class RunArchiver {

    private static final Pattern TURN_ENTRY  = Pattern.compile("turns/(\\d{6,})(?:-compaction-f\\d{6,})?\\.md");
    private static final Pattern PLAIN_TURN_ENTRY = Pattern.compile("turns/(\\d{6,})\\.md");

    private RunArchiver() {}

    /** Zips everything of currentDir into zipFile (an empty zip if currentDir does not exist). */
    static void zip(Path currentDir, Path zipFile) {
        try {
            Files.createDirectories(zipFile.getParent());
            Path tmp = zipFile.resolveSibling(zipFile.getFileName() + ".tmp");
            try (var out = new ZipOutputStream(Files.newOutputStream(tmp))) {
                for (Path file : filesOf(currentDir)) {
                    String name = currentDir.relativize(file).toString().replace('\\', '/');
                    if (!isLegitimate(name)) continue; // un fichier .tmp oublie par un crash n'entre jamais dans un zip
                    out.putNextEntry(new ZipEntry(name));
                    Files.copy(file, out);
                    out.closeEntry();
                }
            }
            Files.move(tmp, zipFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Creation du zip impossible : " + zipFile, e);
        }
    }

    /** Replaces currentDir (deleted first) with the content of zipFile. */
    static void extractInto(Path zipFile, Path currentDir) {
        try (var zip = new ZipFile(zipFile.toFile())) {
            List<? extends ZipEntry> entries = zip.stream().filter(e -> !e.isDirectory()).toList();
            for (ZipEntry entry : entries) {
                if (!isLegitimate(entry.getName())) {
                    throw new IllegalStateException("Entree inattendue dans " + zipFile + " : " + entry.getName());
                }
            }
            deleteRecursively(currentDir);
            Files.createDirectories(currentDir);
            for (ZipEntry entry : entries) {
                Path target = currentDir.resolve(entry.getName());
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du zip impossible : " + zipFile, e);
        }
    }

    /** Highest turn number the zip holds, 0 if it has no turn. Read from the entry names only. */
    static int lastTurnNumber(Path zipFile) {
        int last = 0;
        try (var zip = new ZipFile(zipFile.toFile())) {
            var names = zip.entries();
            while (names.hasMoreElements()) {
                Matcher m = PLAIN_TURN_ENTRY.matcher(names.nextElement().getName());
                if (m.matches()) last = Math.max(last, Integer.parseInt(m.group(1)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du zip impossible : " + zipFile, e);
        }
        return last;
    }

    static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) return;
        try (var paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        } catch (IOException e) {
            throw new UncheckedIOException("Suppression impossible : " + dir, e);
        }
    }

    private static boolean isLegitimate(String entryName) {
        return entryName.equals(RunLayout.STATE_FILE) || TURN_ENTRY.matcher(entryName).matches();
    }

    private static List<Path> filesOf(Path currentDir) throws IOException {
        if (!Files.isDirectory(currentDir)) return List.of();
        try (var paths = Files.walk(currentDir)) {
            List<Path> files = new ArrayList<>(paths.filter(Files::isRegularFile).toList());
            files.sort(Comparator.comparing(Path::toString));
            return files;
        }
    }
}
