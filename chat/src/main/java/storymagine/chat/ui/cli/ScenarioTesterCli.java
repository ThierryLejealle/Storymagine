package storymagine.chat.ui.cli;

import storymagine.chat.ChatModule;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenariotester.ActTestResult;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;
import storymagine.chat.coeur.service.ScenarioTesterService;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.chat.infra.ScenarioTestFileExportAdapter;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.FileLogAdapter;
import storymagine.commun.infra.OllamaEnvDisplay;
import storymagine.commun.infra.TeeLogAdapter;
import storymagine.commun.ui.cli.LlmSetupCli;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Static QA CLI : dry-runs a ChatScenario's acts through ScenarioTesterService (no real chat
 * session, no player), then writes a report.html + logs. Shares the chatscenarios directory
 * convention with ChatCli but never touches a session's history/summary.
 */
public class ScenarioTesterCli {

    /** Opt-in : also write master-log.txt and one file per LLM call next to the report. */
    private static final String LOGS_OPTION = "--logs";

    /** Same shape as FileLogAdapter's timestamped run directory, so report and logs share one folder name. */
    private static final DateTimeFormatter REPORT_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm");

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  Storymagine Chat - Testeur de scénario");
        System.out.println("=================================================");
        OllamaEnvDisplay.print("  ");
        System.out.println();

        boolean    logsEnabled = Arrays.asList(args).contains(LOGS_OPTION);
        Properties props   = ChatCliSupport.loadProperties();
        Scanner    scanner = new Scanner(System.in);
        Path       root    = ChatCliSupport.chooseChatScenariosRoot(props, scanner);

        FileLogAdapter fileLog = new FileLogAdapter();
        LogPort        log     = new TeeLogAdapter(new ConsoleLogAdapter(), fileLog);

        LlmSetupCli.Selection setup = LlmSetupCli.run(props, scanner, log, "chat");
        ModelCallPort         llm   = setup.llm();

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
        ChatScenario scenario;
        try {
            scenario = storage.loadScenario(root, names.get(idx));
        } catch (IllegalStateException e) {
            System.err.println("ERREUR : " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println(">> Chatscenario : " + scenario.name());
        if (scenario.acts().isEmpty()) {
            System.err.println("ERREUR : ce chatscenario n'a pas d'actes — rien à tester acte par acte.");
            System.exit(1);
            return;
        }

        // Le rapport est le produit du testeur : toujours ecrit, dans run/tests/{horodatage}/. Les
        // logs detailles des appels LLM, eux, n'existent que sous --logs (meme dossier horodate).
        Path testsDir = root.resolve(scenario.name()).resolve("run").resolve("tests");
        if (logsEnabled) fileLog.setOutputDir(testsDir);

        System.out.println();
        System.out.println(">> Test de " + scenario.acts().size() + " acte(s)...");
        ScenarioTesterService tester = ChatModule.assembleTester(llm, log);
        ScenarioTestReport    report = tester.testScenario(scenario);

        Path reportDir = logsEnabled && fileLog.runDir() != null
            ? fileLog.runDir()
            : testsDir.resolve(LocalDateTime.now().format(REPORT_STAMP));
        Path reportFile = new ScenarioTestFileExportAdapter().export(report, reportDir);

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


    private static void openBrowser(String url) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (Exception e) {
            System.out.println("(ouverture automatique du navigateur impossible : " + e.getMessage() + ")");
        }
    }
}
