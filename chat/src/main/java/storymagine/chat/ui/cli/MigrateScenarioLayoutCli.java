package storymagine.chat.ui.cli;

import storymagine.chat.infra.ScenarioLayoutMigrator;
import storymagine.chat.infra.ScenarioLayoutMigrator.Deletion;
import storymagine.chat.infra.ScenarioLayoutMigrator.ScenarioMigration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * One-shot migration of chatscenarios from the old flat layout to characters/ + run/ (see
 * ScenarioLayoutMigrator) : character sheets move into characters/, every old game (history,
 * summary, act, presence, saves, archive, logs, tests) is DELETED — only the scenario is kept.
 * Never touches anything without showing it first : lists every move and deletion, then asks for
 * confirmation. Roots processed : paths given as arguments, otherwise the project's chatscenarios/
 * and the directory configured as chatscenarios.root.default. "--dry-run" lists and stops there.
 */
public class MigrateScenarioLayoutCli {

    private static final String DRY_RUN_OPTION = "--dry-run";

    public static void main(String[] args) {
        boolean dryRun = false;
        List<Path> roots = new ArrayList<>();
        for (String arg : args) {
            if (arg.equals(DRY_RUN_OPTION)) dryRun = true;
            else if (!arg.isBlank()) roots.add(Paths.get(arg));
        }
        if (roots.isEmpty()) roots = defaultRoots(ChatCliSupport.loadProperties());

        System.out.println("=================================================");
        System.out.println("  Storymagine Chat - Migration des scenarios");
        System.out.println("=================================================");
        System.out.println("Fiches deplacees dans characters/ ; anciennes parties, sauvegardes,");
        System.out.println("archives et logs SUPPRIMES (seul le scenario est conserve).");
        System.out.println();

        ScenarioLayoutMigrator migrator = new ScenarioLayoutMigrator();
        List<ScenarioMigration> pending = new ArrayList<>();
        int upToDate = 0;
        int blocked = 0;
        for (Path root : roots) {
            System.out.println("Repertoire : " + root.toAbsolutePath());
            if (!Files.isDirectory(root)) {
                System.out.println("  introuvable, ignore.");
                continue;
            }
            for (ScenarioMigration migration : migrator.plan(root)) {
                if (migration.isUpToDate()) { upToDate++; continue; }
                print(migration);
                if (migration.isBlocked()) blocked++;
                else pending.add(migration);
            }
        }

        System.out.println();
        System.out.printf("A migrer : %d | deja a jour : %d | bloques : %d%n", pending.size(), upToDate, blocked);
        if (pending.isEmpty() || dryRun) {
            System.out.println(dryRun ? "Mode --dry-run : rien n'a ete modifie." : "Rien a faire.");
            return;
        }

        System.out.print("Appliquer ? Les suppressions sont definitives (o/n) : ");
        String answer = new Scanner(System.in).nextLine().trim();
        if (!answer.equalsIgnoreCase("o")) {
            System.out.println("Annule, rien n'a ete modifie.");
            return;
        }
        for (ScenarioMigration migration : pending) {
            migrator.execute(migration);
            System.out.println("  migre : " + migration.scenarioName());
        }
        System.out.println("Migration terminee.");
    }

    private static void print(ScenarioMigration migration) {
        System.out.println("  " + migration.scenarioName());
        for (Path sheet : migration.characterSheetsToMove()) {
            System.out.println("    deplace   " + sheet.getFileName() + "  ->  characters/" + sheet.getFileName());
        }
        for (Deletion deletion : migration.deletions()) {
            System.out.printf("    supprime  %-24s %10s%n",
                deletion.path().getFileName() + (deletion.directory() ? "/" : ""), humanSize(deletion.sizeBytes()));
        }
        for (Path stray : migration.leftInPlace()) {
            System.out.println("    laisse    " + stray.getFileName() + "  (pas une fiche : 1re ligne sans \"# Nom\")");
        }
        if (migration.isBlocked()) System.out.println("    BLOQUE    " + migration.conflict());
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return String.format("%.1f Ko", bytes / 1024.0);
        return String.format("%.1f Mo", bytes / (1024.0 * 1024.0));
    }

    /** The project's chatscenarios/ plus chatscenarios.root.default when it points somewhere else. */
    private static List<Path> defaultRoots(Properties props) {
        List<Path> roots = new ArrayList<>();
        Path project = Paths.get("chatscenarios").toAbsolutePath();
        roots.add(project);
        String configured = props.getProperty("chatscenarios.root.default", "").trim();
        if (!configured.isEmpty() && !Paths.get(configured).toAbsolutePath().equals(project)) {
            roots.add(Paths.get(configured));
        }
        return roots;
    }

}
