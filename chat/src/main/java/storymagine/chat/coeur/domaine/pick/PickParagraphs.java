package storymagine.chat.coeur.domaine.pick;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The paragraphs a pick library offers : the content of a picks/{name}.txt file, cut on lines made
 * only of "---" (three dashes or more). Each paragraph is stripped and may span several lines ;
 * blank ones are dropped.
 */
public record PickParagraphs(List<String> values) {

    private static final Pattern SEPARATOR = Pattern.compile("(?m)^-{3,}[ \\t]*$");

    public PickParagraphs {
        values = List.copyOf(values);
    }

    public static PickParagraphs parse(String fileContent) {
        String normalized = fileContent.replace("\r\n", "\n").replace('\r', '\n');
        return new PickParagraphs(Arrays.stream(SEPARATOR.split(normalized))
            .map(String::strip)
            .filter(p -> !p.isEmpty())
            .toList());
    }

    public int size() {
        return values.size();
    }

    public String get(int index) {
        return values.get(index);
    }
}
