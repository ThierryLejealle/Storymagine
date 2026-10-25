package storymagine.chat.infra;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates the skeleton of a new chatscenario : scenario.txt (a premise and two acts, every field a
 * "(...)" instruction still to be replaced, the first act already naming every Npc as guide-redaction-chat.md
 * demands) plus one character sheet per Npc under characters/. Never creates run/ — the game does
 * that — and never overwrites : an existing scenario directory is refused. Pure file creation, no
 * dialogue : the CLI (InitChatCli) asks, this builds. The texts follow chatscenarios/guide-redaction-chat.md
 * (third person, "Quand écrire [NEXT ACT]" on every act but the last).
 */
public final class ScenarioScaffolder {

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    /** What was created : the scenario directory and every file written, in creation order. */
    public record Created(Path scenarioDir, List<Path> files) {}

    /** Letters, digits, '-' and '_' only : the name is also the directory name, so no spaces or accents. */
    public static boolean isValidScenarioName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public Created create(Path chatScenariosRoot, String scenarioName, List<String> npcNames) {
        if (!isValidScenarioName(scenarioName)) {
            throw new IllegalArgumentException("Nom de scenario invalide (lettres, chiffres, - et _ uniquement) : " + scenarioName);
        }
        if (npcNames.isEmpty() || npcNames.stream().anyMatch(n -> n == null || n.isBlank())) {
            throw new IllegalArgumentException("Il faut au moins un PNJ, et chaque PNJ doit avoir un nom.");
        }
        Path dir = chatScenariosRoot.resolve(scenarioName);
        if (Files.exists(dir)) {
            throw new IllegalStateException("Le scenario existe deja, rien n'est ecrase : " + dir);
        }

        List<String> names = npcNames.stream().map(String::strip).toList();
        List<Path> files = new ArrayList<>();
        files.add(write(dir.resolve("scenario.txt"), scenarioText(names)));
        Set<String> usedFileNames = new HashSet<>();
        for (String npcName : names) {
            files.add(write(dir.resolve("characters").resolve(uniqueFileName(npcName, usedFileNames) + ".txt"),
                characterText(npcName)));
        }
        return new Created(dir, files);
    }

    /** "Élise Dupont" -> "elise-dupont" ; a name with no letter or digit at all -> "pnj". */
    static String slug(String npcName) {
        String ascii = Normalizer.normalize(npcName, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "pnj" : slug;
    }

    /** slug, or slug-2, slug-3... when two Npcs would otherwise share a file name. */
    private static String uniqueFileName(String npcName, Set<String> used) {
        String base = slug(npcName);
        String candidate = base;
        for (int n = 2; !used.add(candidate); n++) candidate = base + "-" + n;
        return candidate;
    }

    /** "Elena est là." / "Elena et Marcus sont là." / "Elena, Marcus et Clara sont là." */
    static String presenceSentence(List<String> names) {
        if (names.size() == 1) return names.get(0) + " est là.";
        String head = String.join(", ", names.subList(0, names.size() - 1));
        return head + " et " + names.get(names.size() - 1) + " sont là.";
    }

    private static String scenarioText(List<String> npcNames) {
        return """
            Joueur : Alex

            #SCENARIO
            Monde : (où et quand se déroule l'histoire)
            Prémisse : (la situation de départ, à la 3e personne : "Alex arrive...")
            Ton : (l'ambiance voulue)
            (Optionnel : pour qu'un PNJ prenne la parole en premier à la place d'Alex, écrire [NPC STARTS] seul sur une ligne, ou [NPC STARTS: Nom] pour choisir lequel. Ici pour ouvrir l'histoire, ou dans un acte pour ouvrir cet acte. Supprimer cette consigne sinon.)

            # Premier acte
            [(Qui est Alex et où se trouve-t-il, à la 3e personne.) %s (Le but général d'Alex.)]
            Point de départ : (où en est la scène au début)
            Ce qui doit se passer : (ce que la scène doit accomplir)
            Quand écrire [NEXT ACT] : (la condition précise pour passer à l'acte suivant)

            # Deuxième acte
            [(Ce que le joueur voit quand cet acte commence, à la 3e personne.)]
            Point de départ : (où en est la scène au début)
            Ce qui doit se passer : (ce que la scène doit accomplir)
            """.formatted(presenceSentence(npcNames));
    }

    private static String characterText(String npcName) {
        return """
            # %1$s

            (Ce que tous savent de %1$s : âge, rôle, manière de parler.)

            # SECRET
            (Ce que personne d'autre ne sait sur %1$s. Cette partie est optionnelle : sans la ligne « # SECRET », toute la fiche est publique.)
            """.formatted(npcName);
    }

    private static Path write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Ecriture impossible : " + file, e);
        }
    }
}
