package storymagine.chat.ui.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

/**
 * What every chat CLI (ChatCli, ScenarioTesterCli, InitChatCli, ValidateScenarioCli) shares :
 * loading storymagine.properties + chat.properties, and the interactive choice of the directory
 * that holds the chatscenarios.
 */
final class ChatCliSupport {

    private ChatCliSupport() {}

    /** storymagine.properties (project root, optional) first, then chat.properties on the classpath, which wins. */
    static Properties loadProperties() {
        Properties props = new Properties();
        Path global = Paths.get("storymagine.properties");
        if (Files.exists(global)) {
            try (InputStream is = Files.newInputStream(global)) { props.load(is); }
            catch (IOException ignored) {}
        }
        try (InputStream is = ChatCliSupport.class.getResourceAsStream("/chat.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
        return props;
    }

    /**
     * Asks where the chatscenarios live : ENTER = the configured default (chatscenarios.root.default,
     * else the project's chatscenarios/), 1 = the project's chatscenarios/, 2 = a path typed in. The
     * chosen directory is created if it does not exist yet.
     */
    static Path chooseChatScenariosRoot(Properties props, Scanner scanner) {
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
}
