package storymagine.chat.ui.cli;

import storymagine.chat.infra.ScenarioScaffolder;
import storymagine.chat.infra.ScenarioScaffolder.Created;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Creates the skeleton of a new chatscenario by asking for it : which directory (same choice as
 * chat.bat), the scenario's name, how many Npcs and their names. All the file creation is
 * ScenarioScaffolder's ; this class only holds the dialogue and re-asks until each answer is valid.
 */
public class InitChatCli {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Storymagine Chat - Nouveau scenario");
        System.out.println("=================================================");
        System.out.println();

        Properties props   = ChatCliSupport.loadProperties();
        Scanner    scanner = new Scanner(System.in);
        Path       root    = ChatCliSupport.chooseChatScenariosRoot(props, scanner);

        String name = askScenarioName(scanner, root);
        int npcCount = askNpcCount(scanner);
        List<String> npcNames = askNpcNames(scanner, npcCount);

        Created created = new ScenarioScaffolder().create(root, name, npcNames);

        System.out.println();
        System.out.println("Scenario cree : " + created.scenarioDir().toAbsolutePath());
        for (Path file : created.files()) {
            System.out.println("  " + created.scenarioDir().relativize(file));
        }
        System.out.println();
        System.out.println("Remplace les textes (entre parentheses) dans scenario.txt et chaque fiche,");
        System.out.println("puis lance chat.bat. Regles d'ecriture : chatscenarios/guide-redaction-chat.md");
    }

    private static String askScenarioName(Scanner scanner, Path root) {
        while (true) {
            System.out.print("Nom du scenario (lettres, chiffres, - et _) : ");
            String name = scanner.nextLine().trim();
            if (!ScenarioScaffolder.isValidScenarioName(name)) {
                System.out.println("  --> Nom invalide : ni espace ni accent, seulement lettres, chiffres, - et _.");
            } else if (Files.exists(root.resolve(name))) {
                System.out.println("  --> '" + name + "' existe deja dans ce repertoire, choisir un autre nom.");
            } else {
                return name;
            }
        }
    }

    private static int askNpcCount(Scanner scanner) {
        while (true) {
            System.out.print("Nombre de PNJ (au moins 1) : ");
            String answer = scanner.nextLine().trim();
            try {
                int count = Integer.parseInt(answer);
                if (count >= 1) return count;
            } catch (NumberFormatException ignored) {
                // retombe sur le message ci-dessous
            }
            System.out.println("  --> Entrer un nombre entier, 1 au minimum.");
        }
    }

    private static List<String> askNpcNames(Scanner scanner, int count) {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            while (true) {
                System.out.print("Nom du PNJ " + i + "/" + count + " : ");
                String npcName = scanner.nextLine().trim();
                if (!npcName.isEmpty()) {
                    names.add(npcName);
                    break;
                }
                System.out.println("  --> Le nom ne peut pas etre vide.");
            }
        }
        return names;
    }
}
