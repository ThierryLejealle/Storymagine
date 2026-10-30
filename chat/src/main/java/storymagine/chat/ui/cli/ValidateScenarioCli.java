package storymagine.chat.ui.cli;

import storymagine.chat.coeur.domaine.validation.ScenarioIssue;
import storymagine.chat.coeur.domaine.validation.ScenarioReport;
import storymagine.chat.infra.ChatFileStorageAdapter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Validates scenarios on demand, without any LLM and without touching a run (see
 * ChatStoragePort.validateScenario). Without arguments : the project's chatscenarios/ and the
 * directory configured as chatscenarios.root.default ; with paths as arguments : those directories.
 * Prints every error and warning, one scenario at a time, and ends with exit code 1 if any scenario
 * has an error. The same checks run at every loading of a scenario.
 */
public class ValidateScenarioCli {

    public static void main(String[] args) {
        List<Path> roots = new ArrayList<>();
        for (String arg : args) if (!arg.isBlank()) roots.add(Paths.get(arg));
        if (roots.isEmpty()) roots.addAll(defaultRoots(ChatCliSupport.loadProperties()));

        System.out.println("=================================================");
        System.out.println("  Storymagine Chat - Validation des scenarios");
        System.out.println("=================================================");

        ChatFileStorageAdapter storage = new ChatFileStorageAdapter();
        int scenarios = 0;
        int withErrors = 0;
        int warnings = 0;
        for (Path root : roots) {
            System.out.println();
            System.out.println("Repertoire : " + root.toAbsolutePath());
            if (!Files.isDirectory(root)) {
                System.out.println("  introuvable, ignore.");
                continue;
            }
            for (String name : storage.listScenarioNames(root)) {
                ScenarioReport report = storage.validateScenario(root, name);
                scenarios++;
                if (report.hasErrors()) withErrors++;
                warnings += report.warnings().size();
                System.out.printf("  %-30s %s%n", name, summary(report));
                for (ScenarioIssue issue : report.issues()) System.out.println("      " + issue.display());
            }
        }

        System.out.println();
        System.out.printf("Scenarios : %d | avec erreur : %d | avertissements : %d%n", scenarios, withErrors, warnings);
        if (withErrors > 0) System.exit(1);
    }

    private static String summary(ScenarioReport report) {
        if (report.hasErrors()) return "ERREUR (" + report.errors().size() + ")";
        if (!report.warnings().isEmpty()) return "ok, " + report.warnings().size() + " avertissement(s)";
        return "ok";
    }

    /** The project's chatscenarios/, plus chatscenarios.root.default when it points somewhere else. */
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
