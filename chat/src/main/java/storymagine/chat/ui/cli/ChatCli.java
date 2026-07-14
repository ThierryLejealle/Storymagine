package storymagine.chat.ui.cli;

import storymagine.chat.ChatModule;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.service.ChatService;
import storymagine.chat.infra.ChatFileStorageAdapter;
import storymagine.chat.ui.web.ChatWebServer;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.FileLogAdapter;
import storymagine.commun.infra.ModelHardwareDisplay;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaPsInfo;
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

public class ChatCli {

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  Storymagine Chat");
        System.out.println("=================================================");
        System.out.println();

        Properties props   = loadProperties();
        Scanner    scanner = new Scanner(System.in);
        Path       root    = chooseChatScenariosRoot(props, scanner);

        FileLogAdapter fileLog = new FileLogAdapter();
        LogPort        log     = new TeeLogAdapter(new ConsoleLogAdapter(), fileLog);

        // 1-2. Menu GPU + choix du modele (flux partage, cf. commun/ui/cli/OllamaSetupCli)
        OllamaSetupCli.Selection setup  = OllamaSetupCli.run(props, scanner, log, "chat");
        OllamaAdapter            ollama = setup.llm();

        ChatService service = ChatModule.assemble(ollama, new ChatFileStorageAdapter(), log);

        // 3. Choix du chatscenario
        List<String> names = service.listScenarios(root);
        if (names.isEmpty()) {
            System.err.println("ERREUR : aucun chatscenario dans '" + root + "'.");
            System.err.println("Un chatscenario est un sous-repertoire contenant character.txt et scenario.txt.");
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
        ChatScenario scenario = service.loadScenario(root, names.get(idx));
        System.out.println(">> Chatscenario : " + scenario.name());
        fileLog.setOutputDir(root.resolve(scenario.name()).resolve("logs"));

        // 4. Continuer ou remettre a zero
        System.out.print("Continuer la conversation ? [O/n] : ");
        String contInput = scanner.nextLine().trim();
        boolean reset = contInput.equalsIgnoreCase("n") || contInput.equalsIgnoreCase("non");
        ChatSession session = service.openSession(root, scenario, reset);
        System.out.println(reset
            ? ">> Historique remis a zero."
            : ">> Reprise (" + session.turns().size() + " tour(s) en memoire).");

        // 5. Chargement du modele + info materiel
        System.out.print("Chargement du modele... ");
        try {
            ollama.probe();
            System.out.println("OK");
        } catch (Exception e) {
            System.out.println("(echec probe : " + e.getMessage() + ")");
        }
        OllamaPsInfo ps = OllamaPsInfo.query(setup.ollamaConfig().baseUrl(), setup.modelName()).orElse(null);
        ModelHardwareDisplay.print("[chat] ", ps, NvidiaSmiSnapshot.capture());

        // 6. Serveur web
        int    port = Integer.parseInt(props.getProperty("chat.web.port", "8765"));
        String url  = "http://localhost:" + port;
        ChatWebServer server = new ChatWebServer(service, root, session, port);
        server.start();

        System.out.println();
        System.out.println("=================================================");
        System.out.println("  Chat pret : " + hyperlink(url, url));
        if (fileLog.runDir() != null) {
            System.out.println("  Logs      : " + fileLog.runDir().toAbsolutePath());
        }
        System.out.println("  Ctrl+C pour arreter.");
        System.out.println("=================================================");

        openBrowser(url);
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
        try (InputStream is = ChatCli.class.getResourceAsStream("/chat.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
        return props;
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
