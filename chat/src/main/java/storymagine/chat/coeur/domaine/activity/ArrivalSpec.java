package storymagine.chat.coeur.domaine.activity;

import storymagine.chat.coeur.domaine.scenario.Cast;

import java.util.Arrays;
import java.util.List;

/**
 * What an "{npc-arrives ...}" command names : either characters of the Cast ("Elena", "Elena Marc"),
 * or characters created from a sheet used as a template ("goblin Grik, Snak" : the sheet goblin.txt,
 * once per name, names separated by commas and free to have several words). The first word is the
 * template only when the Cast knows it as one (see Cast.isTemplate) and something follows it.
 * templateId is null for a plain arrival, and names is then the whole text as written.
 */
public record ArrivalSpec(String templateId, List<String> names) {

    public static ArrivalSpec parse(String namesText, Cast cast) {
        String text = namesText == null ? "" : namesText.strip();
        String[] words = text.split("\s+", 2);
        if (words.length == 2 && cast.isTemplate(words[0])) {
            return new ArrivalSpec(words[0], splitNames(words[1]));
        }
        return new ArrivalSpec(null, List.of(text));
    }

    /** True if `text` has the "template Name, Name" shape for the sheet id `templateId` (used before the Cast exists). */
    public static boolean isTemplateForm(String text, String templateId) {
        String[] words = text.strip().split("\s+", 2);
        return words.length == 2 && words[0].equalsIgnoreCase(templateId);
    }

    /** The names written after the template, in order. */
    public static List<String> namesAfterTemplate(String text) {
        String[] words = text.strip().split("\s+", 2);
        return words.length < 2 ? List.of() : splitNames(words[1]);
    }

    private static List<String> splitNames(String text) {
        return Arrays.stream(text.split(",")).map(String::strip).filter(n -> !n.isEmpty()).toList();
    }

    public boolean isTemplate() { return templateId != null; }
}
