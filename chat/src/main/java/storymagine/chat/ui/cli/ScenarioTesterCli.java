package storymagine.chat.ui.cli;

import storymagine.chat.ChatModule;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenariotester.ActTestResult;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;
import storymagine.chat.coeur.service.ScenarioTesterService;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.chat.infra.ScenarioTestFileExportAdapter;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.FileLogAdapter;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.TeeLogAdapter;
import storymagine.commun.ui.cli.OllamaSetupCli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Static QA CLI : dry-runs a ChatScenario's acts through ScenarioTesterService (no real chat
 * session, no player), then writes a report.html + logs. Shares the chatscenarios directory
 * convention with ChatCli but never touches a session's history/summary.
 */
public class ScenarioTesterCli {

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  Storymagine Chat - Testeur de scénario");
        System.out.println("=================================================");
        System.out.println();

        Properties props   = loadProperties();
        Scanner    scanner = new Scanner(System.in);
        Path       root    = chooseChatScenariosRoot(props, scanner);

        FileLogAdapter fileLog = new FileLogAdapter();
        LogPort        log     = new TeeLogAdapter(new ConsoleLogAdapter(), fileLog);

        OllamaSetupCli.Selection setup  = OllamaSetupCli.run(props, scanner, log, "chat");
        OllamaAdapter            ollama = setup.llm();

        ChatFileStorageAdapter storage = new ChatFileStorageAdapter();
        List<String> names = storage.listScenarioNames(root);
        if (names.isEmpty()) {
            System.err.println("ERREUR : aucun chatscenario dans '" + root + "'.");
            System.exit(1);
            return;
        }
        System.out.println();
        System.out.println("Chatscenarios disponibles :");
        for (int i = 0; i < names.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, names.get(i));
        }
        System.out.print("Choix [1] : ");
        String scenInput = scanner.nextLine().trim();
        int idx = scenInput.isBlank() ? 0 : (Integer.parseInt(scenInput) - 1);
        if (idx < 0 || idx >= names.size()) {
            System.err.println("ERREUR : choix invalide.");
            System.exit(1);
            return;
        }
        ChatScenario scenario = storage.loadScenario(root, names.get(idx));
        System.out.println(">> Chatscenario : " + scenario.name());
        if (scenario.acts().isEmpty()) {
            System.err.println("ERREUR : ce chatscenario n'a pas d'actes — rien à tester acte par acte.");
            System.exit(1);
            return;
        }

        fileLog.setOutputDir(root.resolve(scenario.name()).resolve("tests"));

        System.out.println();
        System.out.println(">> Test de " + scenario.acts().size() + " acte(s)...");
        ScenarioTesterService tester = ChatModule.assembleTester(ollama, log);
        ScenarioTestReport    report = tester.testScenario(scenario);

        Path reportFile = new ScenarioTestFileExportAdapter().export(report, fileLog.runDir());

        System.out.println();
        System.out.println("=================================================");
        System.out.println("  Rapport : " + reportFile.toAbsolutePath());
        printSummary(report);
        System.out.println("=================================================");

        openBrowser(reportFile.toUri().toString());
    }

    private static void printSummary(ScenarioTestReport report) {
        int continuity = 0, clarity = 0, suggestions = 0;
        for (ActTestResult act : report.actResults()) {
            continuity  += act.continuityIssues().size();
            clarity     += act.clarityIssues().size();
            suggestions += act.suggestions().size();
        }
        System.out.printf("  %d acte(s) testé(s) — %d incohérence(s), %d problème(s) de clarté, %d suggestion(s)%n",
            report.actResults().size(), continuity, clarity, suggestions);
    }

    private static Path chooseChatScenariosRoot(Properties props, Scanner scanner) {
        String defaultDir = props.getProperty("chatscenarios.root.default", "");
        String projectDir = Paths.get("chatscenarios").toAbsolutePath().toString();

        System.out.println("Ou se trouvent les chatscenarios ?");
        System.out.println("  ENTREE - Repertoire par defaut : " + (defaultDir.isBlank() ? projectDir : defaultDir));
        System.out.println("  1      - Repertoire du projet  : " + projectDir);
        System.out.println("  2      - Autre repertoire...");
        System.out.println();

        while (true) {
            System.out.print("Choix [ENTREE/1/2] : ");
            String choice = scanner.nextLine().trim();
            Path root = switch (choice) {
                case ""  -> Paths.get(defaultDir.isBlank() ? projectDir : defaultDir);
                case "1" -> Paths.get(projectDir);
                case "2" -> {
                    System.out.print("Chemin du repertoire : ");
                    yield Paths.get(scanner.nextLine().trim());
                }
                default -> null;
            };
            if (root == null) {
                System.out.println("  --> Entree invalide, choisir ENTREE, 1 ou 2.");
                continue;
            }
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                System.err.println("ERREUR : impossible de creer '" + root + "' : " + e.getMessage());
                System.exit(1);
            }
            System.out.println();
            return root;
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        Path global = Paths.get("storymagine.properties");
        if (Files.exists(global)) {
            try (InputStream is = Files.newInputStream(global)) { props.load(is); }
            catch (IOException ignored) {}
        }
        try (InputStream is = ScenarioTesterCli.class.getResourceAsStream("/chat.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
        return props;
    }

    private static void openBrowser(String url) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (Exception e) {
            System.out.println("(ouverture automatique du navigateur impossible : " + e.getMessage() + ")");
        }
    }
}
