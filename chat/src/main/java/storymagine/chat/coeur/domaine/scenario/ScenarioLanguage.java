package storymagine.chat.coeur.domaine.scenario;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * The language every Npc must write its replies in, read from an optional "Langue : ..." header
 * line of scenario.txt (see ChatFileStorageAdapter). englishName is how a prompt names it ("French"),
 * so the instruction itself stays in the prompt's own language. UNSPECIFIED (no header line) leaves
 * the language to the model, as before. Accepted spellings : ISO code, English name or French name
 * of the six common languages, any case, accents ignored ; any other text is used verbatim.
 */
public record ScenarioLanguage(String englishName) {

    public static final ScenarioLanguage UNSPECIFIED = new ScenarioLanguage("");

    private static final Map<String, String> KNOWN = Map.ofEntries(
        Map.entry("fr", "French"),     Map.entry("french", "French"),     Map.entry("francais", "French"),
        Map.entry("en", "English"),    Map.entry("english", "English"),   Map.entry("anglais", "English"),
        Map.entry("es", "Spanish"),    Map.entry("spanish", "Spanish"),   Map.entry("espagnol", "Spanish"),
        Map.entry("de", "German"),     Map.entry("german", "German"),     Map.entry("allemand", "German"),
        Map.entry("it", "Italian"),    Map.entry("italian", "Italian"),   Map.entry("italien", "Italian"),
        Map.entry("pt", "Portuguese"), Map.entry("portuguese", "Portuguese"), Map.entry("portugais", "Portuguese")
    );

    public ScenarioLanguage {
        if (englishName == null) englishName = "";
    }

    /** Blank or null gives UNSPECIFIED. */
    public static ScenarioLanguage parse(String raw) {
        if (raw == null || raw.isBlank()) return UNSPECIFIED;
        String text = raw.strip();
        String key = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return new ScenarioLanguage(KNOWN.getOrDefault(key, text));
    }

    public boolean isSpecified() {
        return !englishName.isBlank();
    }

    /**
     * The words that complete "Write ... in " in an English prompt, so every agent states the language
     * the same way : "French, even though these instructions are in English" (the rule is fixed, the
     * model has nothing to infer). UNSPECIFIED gives unspecifiedPhrase, the agent's former
     * "the same language as ..." wording.
     */
    public String phrase(String unspecifiedPhrase) {
        return isSpecified() ? englishName + ", even though these instructions are in English" : unspecifiedPhrase;
    }
}
