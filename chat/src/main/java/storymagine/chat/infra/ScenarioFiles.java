package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenario.ActNumber;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The text files of a scenario folder, in reading order : scenario.txt first, then every "scenario-NUMBER.txt"
 * sorted by their part number. A part number is dotted like an outline number (1, 1.1, 1.1.1, 2, 3.1.2...) and
 * sorts the same way : a number comes before its children, and the segments compare as numbers (1.2 before
 * 1.10). Gaps are fine and 1 may live next to 1.1 : an author who splits a file or adds one in the middle
 * never has to rename the others. The number only gives the order, it does not have to match the numbers of
 * the acts (those come from the headings, see ScenarioOutlineParser).
 */
final class ScenarioFiles {

    static final String MAIN_FILE = "scenario.txt";
    private static final Pattern PART_FILE = Pattern.compile("scenario-(\\d{1,6}(?:\\.\\d{1,6})*)\\.txt");

    private ScenarioFiles() {}

    /** True for a "scenario-NUMBER.txt" file name (continuation of scenario.txt). */
    static boolean isPart(String fileName) {
        return PART_FILE.matcher(fileName).matches();
    }

    /**
     * scenario.txt (listed even if absent) followed by the part files found in the folder, in order.
     * @throws IllegalArgumentException two files with the same number (scenario-1.txt and scenario-01.txt)
     */
    static List<Path> textFiles(Path dir) {
        List<Path> files = new ArrayList<>(List.of(dir.resolve(MAIN_FILE)));
        List<Part> parts = new ArrayList<>();
        try (Stream<Path> entries = Files.list(dir)) {
            entries.filter(Files::isRegularFile).forEach(file -> {
                Matcher m = PART_FILE.matcher(file.getFileName().toString());
                if (m.matches()) parts.add(new Part(numberOf(m.group(1)), file));
            });
        } catch (IOException e) {
            return files; // no folder to read : only the main file is named, the caller reports it missing
        }
        parts.sort(Comparator.comparing(Part::number));
        for (int i = 1; i < parts.size(); i++) {
            if (parts.get(i).number().equals(parts.get(i - 1).number())) {
                throw new IllegalArgumentException(parts.get(i - 1).file().getFileName() + " et " + parts.get(i).file().getFileName()
                    + " portent le meme numero (" + parts.get(i).number().display() + ") : un seul fichier par numero.");
            }
        }
        parts.forEach(part -> files.add(part.file()));
        return files;
    }

    private static ActNumber numberOf(String dotted) {
        return new ActNumber(Stream.of(dotted.split("\\.")).map(Integer::parseInt).toList());
    }

    private record Part(ActNumber number, Path file) {}
}
