package storymagine.chat.ui.cli;

import storymagine.chat.ChatModule;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.service.ChatService;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.chat.ui.web.ChatWebServer;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.FileLogAdapter;
import storymagine.commun.infra.ModelHardwareDisplay;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaEnvDisplay;
import storymagine.commun.infra.OllamaPsInfo;
import storymagine.commun.infra.TeeLogAdapter;
import storymagine.commun.ui.cli.LlmSetupCli;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class ChatCli {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Opt-in : write master-log.txt and one file per LLM call under the scenario's run/logs/. */
    private static final String LOGS_OPTION = "--logs";

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  Storymagine Chat");
        System.out.println("=================================================");
        OllamaEnvDisplay.print("  ");
        System.out.println();

        boolean    logsEnabled = Arrays.asList(args).contains(LOGS_OPTION);
        Properties props   = ChatCliSupport.loadProperties();
        Scanner    scanner = new Scanner(System.in);
        Path       root    = ChatCliSupport.chooseChatScenariosRoot(props, scanner);

        FileLogAdapter fileLog = new FileLogAdapter();
        LogPort        log     = new TeeLogAdapter(new ConsoleLogAdapter(), fileLog);

        // 1-2. Choix du serveur LLM (Ollama ou llama-server) + modele (flux partage, cf. commun/ui/cli/LlmSetupCli)
        LlmSetupCli.Selection setup = LlmSetupCli.run(props, scanner, log, "chat");
        ModelCallPort         llm   = setup.llm();

        ChatService service = ChatModule.assemble(llm, new ChatFileStorageAdapter(), log);

        // 3. Choix du chatscenario
        List<String> names = service.listScenarios(root);
        if (names.isEmpty()) {
            System.err.println("ERREUR : aucun chatscenario dans '" + root + "'.");
            System.err.println("Un chatscenario est un sous-repertoire contenant scenario.txt et un dossier characters/ avec au moins une fiche.");
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
        String scenarioName = names.get(idx);

        // 4. Continuer ou remettre a zero — AVANT de charger le scenario : le chargement relit le run
        // (copie resolue du scenario, etat) et un scenario copie avec son run puis adapte peut y echouer.
        // Si on repart de zero, le run est archive d'abord et plus rien ne peut bloquer. La question
        // n'a de sens que s'il y a une partie a reprendre ; sans historique, rien a demander.
        boolean reset = false;
        if (service.hasSavedSession(root, scenarioName)) {
            System.out.print("Continuer la conversation ? [O/n] : ");
            String contInput = scanner.nextLine().trim();
            reset = contInput.equalsIgnoreCase("n") || contInput.equalsIgnoreCase("non");
            if (reset) {
                service.archiveRun(root, scenarioName);
                System.out.println(">> Historique remis a zero (ancienne partie archivee dans run/archive).");
            }
        }
        ChatScenario scenario = loadOrOfferRestart(service, root, scenarioName, reset, scanner);
        if (scenario == null) {
            System.exit(1);
            return;
        }
        System.out.println(">> Chatscenario : " + scenario.name());
        service.scenarioWarnings(scenario).forEach(w -> System.out.println("   " + w));
        // Aucun log sur disque par defaut : seule l'option --logs active FileLogAdapter (qui
        // ignore toutes les ecritures tant qu'on ne lui a pas donne de repertoire).
        Path logsDir = root.resolve(scenario.name()).resolve("run").resolve("logs");
        if (logsEnabled) fileLog.setOutputDir(logsDir);

        boolean resume = service.hasSavedSession(root, scenario);
        ChatSession session = service.openSession(root, scenario, false);
        if (resume) {
            System.out.println(">> Reprise (" + session.turns().stream()
                .filter(t -> t.speaker() != ChatTurn.Speaker.NARRATOR).count() + " tour(s) joue(s) en memoire).");
        } else if (!reset) {
            System.out.println(">> Nouvelle partie.");
        }

        // 5. Chargement du modele + info materiel
        System.out.printf("[%s] Chargement du modele...%n", ts());
        long probeT0 = System.currentTimeMillis();
        try {
            setup.lifecycle().probe();
            System.out.printf("[%s] Modele charge (%dms).%n", ts(), System.currentTimeMillis() - probeT0);
        } catch (Exception e) {
            System.out.printf("[%s] (echec probe apres %dms : %s)%n",
                ts(), System.currentTimeMillis() - probeT0, e.getMessage());
        }
        OllamaPsInfo ps = null;
        if ("ollama".equals(setup.provider())) {
            System.out.printf("[%s] Interrogation /api/ps...%n", ts());
            ps = OllamaPsInfo.query(setup.baseUrl(), setup.modelName()).orElse(null);
        }
        ModelHardwareDisplay.print("[chat] ", ps, NvidiaSmiSnapshot.capture());

        // 6. Serveur web
        int preferredPort = Integer.parseInt(props.getProperty("chat.web.port", "8765"));
        System.out.printf("[%s] Demarrage du serveur web (port prefere %d)...%n", ts(), preferredPort);
        int    port = new ChatWebServer(service, root, session, preferredPort).start();
        String url  = "http://localhost:" + port;
        if (port != preferredPort) {
            System.out.printf("[%s] Port %d occupe (autre chat ?), utilisation du port %d.%n", ts(), preferredPort, port);
        }
        System.out.printf("[%s] Serveur web pret.%n", ts());

        System.out.println();
        System.out.println("=================================================");
        System.out.println("  Chat pret : " + hyperlink(url, url));
        if (logsEnabled) {
            System.out.println("  Logs      : " + logsDir.toAbsolutePath());
        }
        System.out.println("  Ctrl+C pour arreter.");
        System.out.println("=================================================");

        openBrowser(url);
    }

    private static String ts() {
        return LocalTime.now().format(TIME);
    }


    /** OSC 8 terminal hyperlink — rendu cliquable par Windows Terminal, silencieusement ignore par conhost classique. */
    private static String hyperlink(String url, String label) {
        return "]8;;" + url + "\\" + label + "]8;;\\";
    }

    private static void openBrowser(String url) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", url).start();
        } catch (Exception e) {
            System.out.println("(ouverture automatique du navigateur impossible : " + e.getMessage() + ")");
        }
    }

    /**
     * Loads the scenario. If that fails while a run exists on disk, says so and offers to archive that
     * run and try again (the error may come from the run itself, for example a scenario copied with its
     * run/ then adapted). Null when the scenario cannot be loaded : the error was printed.
     */
    private static ChatScenario loadOrOfferRestart(ChatService service, Path root, String name, boolean alreadyReset, Scanner scanner) {
        try {
            return service.loadScenario(root, name);
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.err.println("ERREUR : " + e.getMessage());
            if (alreadyReset || !service.hasRun(root, name)) return null;
            System.out.println("Cette erreur peut venir de la partie enregistree (run/), par exemple copiee d'un autre scenario.");
            System.out.print("La remettre a zero (archivee dans run/archive) et recharger ? [o/N] : ");
            String answer = scanner.nextLine().trim();
            if (!answer.equalsIgnoreCase("o") && !answer.equalsIgnoreCase("oui")) return null;
            service.archiveRun(root, name);
            System.out.println(">> Ancienne partie archivee dans run/archive.");
            try {
                return service.loadScenario(root, name);
            } catch (IllegalStateException | IllegalArgumentException again) {
                System.err.println("ERREUR : " + again.getMessage());
                return null;
            }
        }
    }
}
