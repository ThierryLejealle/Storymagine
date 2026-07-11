package storymagine.commun.coeur.domaine.text;

import java.util.Map;

/**
 * ISO language code -> English name, for prompts that need a dynamic "write in {language}"
 * instruction instead of a hardcoded language. Defaults to French (every scenario in this
 * project assumes French unless configured otherwise).
 */
public final class LanguageNames {

    private static final Map<String, String> NAMES = Map.of(
        "fr", "French",
        "en", "English",
        "es", "Spanish",
        "de", "German",
        "it", "Italian",
        "pt", "Portuguese"
    );

    private LanguageNames() {
    }

    public static String english(String isoCode) {
        if (isoCode == null || isoCode.isBlank()) return "French";
        return NAMES.getOrDefault(isoCode.trim().toLowerCase(), "French");
    }
}
