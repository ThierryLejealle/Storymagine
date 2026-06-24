package storymagine.redacteur.ui.cli;

import storymagine.redacteur.coeur.service.InitStoryCommand;
import storymagine.redacteur.coeur.service.InitStoryResult;
import storymagine.redacteur.coeur.service.InitStoryService;
import storymagine.redacteur.coeur.service.InitStoryServiceImpl;
import storymagine.redacteur.infra.StoryTemplateFileAdapter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class InitStoryCli {

    private static final String DEFAULT_STORY_ROOT = "C:\\dev\\llm\\story";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Storymagine - Nouvelle histoire");
        System.out.println("=================================================");
        System.out.println();

        String templateProp = System.getProperty("init.template.dir");
        Path templateDir = (templateProp != null && !templateProp.isBlank())
            ? Paths.get(templateProp)
            : Paths.get("story", "modele");

        InitStoryService service = new InitStoryServiceImpl(new StoryTemplateFileAdapter(templateDir));
        Scanner scanner = new Scanner(System.in);

        // 1. Repertoire racine des histoires
        System.out.println("Repertoire des histoires [defaut : " + DEFAULT_STORY_ROOT + "]");
        System.out.print("Autre repertoire (ENTREE pour le defaut) : ");
        String rootInput = scanner.nextLine().trim();
        Path storyRoot = rootInput.isBlank() ? Paths.get(DEFAULT_STORY_ROOT) : Paths.get(rootInput);

        if (!storyRoot.toFile().exists()) {
            System.out.println();
            System.out.println("Repertoire inexistant - creation de : " + storyRoot);
            if (!storyRoot.toFile().mkdirs()) {
                System.err.println("ERREUR : impossible de creer le repertoire " + storyRoot);
                System.exit(1);
                return;
            }
            System.out.println("Repertoire cree.");
        }

        System.out.println();
        System.out.println("Repertoire : " + storyRoot.toAbsolutePath());
        System.out.println();

        // 2. Nom de l'histoire
        String bookName;
        while (true) {
            System.out.print("Nom de l'histoire : ");
            bookName = scanner.nextLine().trim();
            if (!bookName.isBlank()) break;
            System.out.println("  --> Le nom ne peut pas etre vide.");
        }

        // 3. Verification ecrasement
        Path bookDir = storyRoot.resolve(bookName);
        boolean overwrite = false;
        if (bookDir.toFile().exists()) {
            System.out.println();
            System.out.println("ATTENTION : le repertoire \"" + bookDir + "\" existe deja.");
            System.out.print("Ecraser le contenu existant ? [o/N] : ");
            String answer = scanner.nextLine().trim();
            if (!answer.equalsIgnoreCase("o")) {
                System.out.println("Annule.");
                return;
            }
            overwrite = true;
        }

        // 4. Initialisation
        System.out.println();
        System.out.println("Initialisation de \"" + bookName + "\"...");
        InitStoryResult result;
        try {
            result = service.init(new InitStoryCommand(storyRoot, bookName, overwrite));
        } catch (Exception e) {
            System.err.println("ERREUR : " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println();
        System.out.println("Histoire initialisee dans : " + result.bookDir().toAbsolutePath());
        System.out.println();
        System.out.println("Etapes suivantes :");
        System.out.println("  1. Editez " + result.bookDir() + "\\scenario.md");
        System.out.println("  2. Editez " + result.bookDir() + "\\goal.md");
        System.out.println("  3. Ajoutez vos chapitres dans " + result.bookDir() + "\\chapitres\\");
        System.out.println("  4. Lancez lancer.bat pour la generation");
        System.out.println();
    }
}
