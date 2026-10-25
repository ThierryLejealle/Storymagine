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
        ChatScenario scenario;
        try {
            scenario = service.loadScenario(root, names.get(idx));
        } catch (IllegalStateException e) {
            System.err.println("ERREUR : " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println(">> Chatscenario : " + scenario.name());
        // Aucun log sur disque par defaut : seule l'option --logs active FileLogAdapter (qui
        // ignore toutes les ecritures tant qu'on ne lui a pas donne de repertoire).
        Path logsDir = root.resolve(scenario.name()).resolve("run").resolve("logs");
        if (logsEnabled) fileLog.setOutputDir(logsDir);

        // 4. Continuer ou remettre a zero — la question n'a de sens que s'il y a une partie a
        // reprendre ; sans historique sur disque c'est un demarrage, rien a demander.
        ChatSession session;
        if (service.hasSavedSession(root, scenario)) {
            System.out.print("Continuer la conversation ? [O/n] : ");
            String contInput = scanner.nextLine().trim();
            boolean reset = contInput.equalsIgnoreCase("n") || contInput.equalsIgnoreCase("non");
            session = service.openSession(root, scenario, reset);
            System.out.println(reset
                ? ">> Historique remis a zero."
                : ">> Reprise (" + session.turns().stream()
                    .filter(t -> t.speaker() != ChatTurn.Speaker.NARRATOR).count() + " tour(s) joue(s) en memoire).");
        } else {
            session = service.openSession(root, scenario, false);
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
}
