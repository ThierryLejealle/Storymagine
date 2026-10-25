package storymagine.commun.ui.cli;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.ModelLifecyclePort;

import java.util.Properties;
import java.util.Scanner;

/**
 * Menu de tête commun à tout module CLI pilotant un LLM : choix du serveur (Ollama ou
 * llama-server), puis délégation au flux d'installation propre à chacun (OllamaSetupCli —
 * menu GPU + catalogue de modèles ; LlamaServerSetupCli — simple vérification de connexion, un
 * seul modèle déjà chargé). Le résultat est exposé via les ports communs (ModelCallPort /
 * ModelLifecyclePort) : le reste du programme n'a pas besoin de savoir lequel a été choisi, sauf
 * pour les quelques features propres à Ollama (ex. /api/ps, voir provider()).
 */
public final class LlmSetupCli {

    private LlmSetupCli() {}

    public record Selection(ModelCallPort llm, ModelLifecyclePort lifecycle, String modelName,
                             String provider, String baseUrl) {}

    public static Selection run(Properties props, Scanner scanner, LogPort log, String appLabel) {
        System.out.println("Quel serveur LLM utiliser ?");
        System.out.println("  1. Ollama");
        System.out.println("  2. llama-server");

        int choice = 0;
        while (choice < 1 || choice > 2) {
            System.out.print("Choix [1-2] : ");
            String line = scanner.nextLine().trim();
            try { choice = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
            if (choice < 1 || choice > 2) System.out.println("  Entree invalide, choisir 1 ou 2.");
        }
        System.out.println();

        if (choice == 1) {
            OllamaSetupCli.Selection s = OllamaSetupCli.run(props, scanner, log, appLabel);
            return new Selection(s.llm(), s.llm(), s.modelName(), "ollama", s.ollamaConfig().baseUrl());
        } else {
            LlamaServerSetupCli.Selection s = LlamaServerSetupCli.run(props, log);
            return new Selection(s.llm(), s.llm(), s.modelName(), "llamaserver", s.config().baseUrl());
        }
    }
}
