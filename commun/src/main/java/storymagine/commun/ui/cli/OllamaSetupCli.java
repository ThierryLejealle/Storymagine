package storymagine.commun.ui.cli;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaConfig;
import storymagine.commun.infra.OllamaLauncher;
import storymagine.commun.infra.RetryPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Console flow shared by every module CLI that drives Ollama : GPU menu, restart on the chosen
 * GPU, then model listing (favoris first) and selection. Extracted from RedacteurCli so redacteur
 * and chat (and any future module) don't duplicate it.
 */
public final class OllamaSetupCli {

    private OllamaSetupCli() {}

    /** Outcome of the interactive setup : ready-to-use adapter, chosen model name, and the config it came from. */
    public record Selection(OllamaAdapter llm, String modelName, OllamaConfig ollamaConfig) {}

    public static Selection run(Properties props, Scanner scanner, LogPort log, String appLabel) {
        OllamaConfig ollama = buildOllamaConfig(props);
        chooseGpu(ollama, scanner, appLabel);
        String model = chooseModel(ollama, props, scanner);
        return new Selection(ollama.adapter(model, log), model, ollama);
    }

    public static OllamaConfig buildOllamaConfig(Properties props) {
        String  baseUrl             = props.getProperty("ollama.base-url",                        "http://localhost:11434");
        int     ctxWindow           = Integer.parseInt(props.getProperty("ollama.context-window",                  "32768"));
        int     maxCtx              = Integer.parseInt(props.getProperty("ollama.max-context-window",             "131072"));
        int     topK                = Integer.parseInt(props.getProperty("ollama.top-k",                              "40"));
        double  topP                = Double.parseDouble(props.getProperty("ollama.top-p",                           "0.9"));
        double  repeatPenalty       = Double.parseDouble(props.getProperty("ollama.repeat-penalty",                  "1.1"));
        int     numPredict          = Integer.parseInt(props.getProperty("ollama.num-predict",                         "-1"));
        boolean streamMode          = !"sync".equalsIgnoreCase(props.getProperty("ollama.mode",                   "stream"));
        int     timeoutMs           = Integer.parseInt(props.getProperty("ollama.timeout-ms",                     "600000"));
        int     firstTokenTimeoutMs = Integer.parseInt(props.getProperty("ollama.stream.first-token-timeout-ms", "300000"));
        int     interTokenTimeoutMs = Integer.parseInt(props.getProperty("ollama.stream.inter-token-timeout-ms",   "30000"));
        int     ramFraction         = Integer.parseInt(props.getProperty("ollama.large-model-ram-fraction-pct",       "60"));
        int     timeoutMult         = Integer.parseInt(props.getProperty("ollama.large-model-timeout-multiplier",      "3"));
        int     retryCount          = Integer.parseInt(props.getProperty("ollama.retry-count",                          "5"));
        int     retryDelay1         = Integer.parseInt(props.getProperty("ollama.retry-delay1",                        "15"));
        int     retryDelay2         = Integer.parseInt(props.getProperty("ollama.retry-delay2",                        "30"));
        int     retryDelay3         = Integer.parseInt(props.getProperty("ollama.retry-delay3",                        "60"));

        return new OllamaConfig(baseUrl, ctxWindow, maxCtx, topK, topP, repeatPenalty,
            numPredict, streamMode, timeoutMs, firstTokenTimeoutMs, interTokenTimeoutMs,
            new RetryPolicy(retryCount, retryDelay1, retryDelay2, retryDelay3),
            ramFraction, timeoutMult);
    }

    public static List<String> loadFavoris(Properties props) {
        List<String> result = new ArrayList<>();
        for (int i = 1; ; i++) {
            String val = props.getProperty("favoris." + i);
            if (val == null || val.isBlank()) break;
            result.add(val.trim());
        }
        return result;
    }

    private static void chooseGpu(OllamaConfig ollama, Scanner scanner, String appLabel) {
        List<NvidiaSmiSnapshot.GpuStat> gpus = NvidiaSmiSnapshot.capture();
        System.out.println("Configuration GPU Ollama :");
        System.out.printf("  1. Utiliser Ollama deja demarre%n");
        printGpuOption(2, gpus, 0);
        printGpuOption(3, gpus, 1);
        System.out.printf("  4. Utiliser GPU 0+1 - split auto%n");

        int gpuChoice = 0;
        while (gpuChoice < 1 || gpuChoice > 4) {
            System.out.print("Choix GPU [1-4] : ");
            String line = scanner.nextLine().trim();
            try { gpuChoice = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
            if (gpuChoice < 1 || gpuChoice > 4) System.out.println("  Entree invalide, choisir 1-4.");
        }
        System.out.println();

        switch (gpuChoice) {
            case 1 -> {
                if (!OllamaLauncher.isReachable(ollama.baseUrl())) {
                    System.err.println("ERREUR : Ollama non joignable. Lancez Ollama d'abord ou choisissez 2-4.");
                    System.exit(1);
                }
                System.out.printf("[%s] Ollama deja demarre.%n%n", appLabel);
            }
            case 2 -> {
                System.out.printf("[%s] Relancement Ollama sur GPU 0...%n", appLabel);
                try { OllamaLauncher.killAll(); OllamaLauncher.launch("0", ollama.baseUrl()); }
                catch (Exception e) { System.err.println("ERREUR : " + e.getMessage()); System.exit(1); }
                System.out.printf("[%s] Ollama pret - %s%n%n", appLabel, gpuLabel(gpus, 0));
            }
            case 3 -> {
                System.out.printf("[%s] Relancement Ollama sur GPU 1...%n", appLabel);
                try { OllamaLauncher.killAll(); OllamaLauncher.launch("1", ollama.baseUrl()); }
                catch (Exception e) { System.err.println("ERREUR : " + e.getMessage()); System.exit(1); }
                System.out.printf("[%s] Ollama pret - %s%n%n", appLabel, gpuLabel(gpus, 1));
            }
            default -> {
                System.out.printf("[%s] Relancement Ollama sur GPU 0+1...%n", appLabel);
                try { OllamaLauncher.killAll(); OllamaLauncher.launch("0,1", ollama.baseUrl()); }
                catch (Exception e) { System.err.println("ERREUR : " + e.getMessage()); System.exit(1); }
                System.out.printf("[%s] Ollama pret - GPU 0+1 split%n%n", appLabel);
            }
        }
    }

    private static String chooseModel(OllamaConfig ollama, Properties props, Scanner scanner) {
        System.out.println("Connexion a Ollama (" + ollama.baseUrl() + ")...");
        OllamaAdapter probe = ollama.adapter("_probe");
        List<ModelEntry> allModels;
        try {
            allModels = probe.listModels(Long.MAX_VALUE);
        } catch (Exception e) {
            System.err.println("ERREUR : impossible de contacter Ollama : " + e.getMessage());
            System.exit(1);
            return null;
        }
        if (allModels.isEmpty()) {
            System.err.println("ERREUR : aucun modele disponible dans Ollama.");
            System.exit(1);
            return null;
        }

        List<String> favoris = loadFavoris(props);
        List<ModelEntry> sorted = new ArrayList<>();
        for (String fav : favoris) {
            allModels.stream().filter(m -> m.name().equals(fav)).findFirst().ifPresent(sorted::add);
        }
        for (ModelEntry m : allModels) {
            if (sorted.stream().noneMatch(s -> s.name().equals(m.name()))) sorted.add(m);
        }

        System.out.println();
        System.out.println("Modeles disponibles :");
        for (int i = 0; i < sorted.size(); i++) {
            ModelEntry m      = sorted.get(i);
            boolean    isFav  = i < favoris.size() && favoris.contains(m.name());
            String     marker = isFav ? " *" : "  ";
            System.out.printf("%s %2d. %-60s %s%n", marker, i + 1, m.name(), formatSize(m.sizeBytes()));
        }
        System.out.println("  (* = favori)");
        System.out.print("Choix du modele [1] : ");
        String modelInput = scanner.nextLine().trim();
        int modelIdx = modelInput.isBlank() ? 0 : (Integer.parseInt(modelInput) - 1);
        if (modelIdx < 0 || modelIdx >= sorted.size()) {
            System.err.println("ERREUR : choix invalide.");
            System.exit(1);
        }
        String selectedModel = sorted.get(modelIdx).name();
        System.out.println(">> Modele : " + selectedModel);
        return selectedModel;
    }

    private static void printGpuOption(int num, List<NvidiaSmiSnapshot.GpuStat> gpus, int gpuIndex) {
        NvidiaSmiSnapshot.GpuStat g = gpus.stream()
                .filter(x -> x.index() == gpuIndex).findFirst().orElse(null);
        if (g != null) {
            System.out.printf("  %d. Utiliser GPU %d - %-32s  %.0f Go%n",
                    num, gpuIndex, g.name(), g.totalMb() / 1024.0);
        } else {
            System.out.printf("  %d. Utiliser GPU %d (CUDA_VISIBLE_DEVICES=%d)%n",
                    num, gpuIndex, gpuIndex);
        }
    }

    private static String gpuLabel(List<NvidiaSmiSnapshot.GpuStat> gpus, int index) {
        return gpus.stream()
                .filter(g -> g.index() == index)
                .findFirst()
                .map(g -> "GPU " + index + " - " + g.name())
                .orElse("GPU " + index);
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "";
        if (bytes >= 1_000_000_000L) return String.format("(%.1f Go)", bytes / 1e9);
        return String.format("(%.0f Mo)", bytes / 1e6);
    }
}
