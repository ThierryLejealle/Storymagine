package storymagine.redacteur.ui.cli;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.FileLogAdapter;
import storymagine.commun.infra.ModelHardwareDisplay;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaConfig;
import storymagine.commun.infra.OllamaLauncher;
import storymagine.commun.infra.OllamaPsInfo;
import storymagine.commun.infra.RetryPolicy;
import storymagine.commun.infra.TeeLogAdapter;
import storymagine.redacteur.RedacteurModule;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.BeatsConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.write.CorrectorConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.infra.HtmlFileExportAdapter;
import storymagine.redacteur.infra.StoryExporter;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class RedacteurCli {

    public static void main(String[] args) throws IOException {
        System.out.println("=================================================");
        System.out.println("  Storymagine Redacteur");
        System.out.println("=================================================");
        System.out.println();

        Properties   props   = loadProperties();
        Scanner      scanner = new Scanner(System.in);
        Path         scenarioRoot = chooseScenarioRoot(props, scanner);
        OllamaConfig ollama  = buildOllamaConfig(props);
        List<String> favoris = loadFavoris(props);

        FileLogAdapter fileLog = new FileLogAdapter();
        LogPort        log     = new TeeLogAdapter(new ConsoleLogAdapter(), fileLog);

        // 1. Menu GPU
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
                System.out.printf("[redacteur] Ollama deja demarre.%n%n");
            }
            case 2 -> {
                System.out.printf("[redacteur] Relancement Ollama sur GPU 0...%n");
                try { OllamaLauncher.killAll(); OllamaLauncher.launch("0", ollama.baseUrl()); }
                catch (Exception e) { System.err.println("ERREUR : " + e.getMessage()); System.exit(1); }
                System.out.printf("[redacteur] Ollama pret - %s%n%n", gpuLabel(gpus, 0));
            }
            case 3 -> {
                System.out.printf("[redacteur] Relancement Ollama sur GPU 1...%n");
                try { OllamaLauncher.killAll(); OllamaLauncher.launch("1", ollama.baseUrl()); }
                catch (Exception e) { System.err.println("ERREUR : " + e.getMessage()); System.exit(1); }
                System.out.printf("[redacteur] Ollama pret - %s%n%n", gpuLabel(gpus, 1));
            }
            default -> {
                System.out.printf("[redacteur] Relancement Ollama sur GPU 0+1...%n");
                try { OllamaLauncher.killAll(); OllamaLauncher.launch("0,1", ollama.baseUrl()); }
                catch (Exception e) { System.err.println("ERREUR : " + e.getMessage()); System.exit(1); }
                System.out.printf("[redacteur] Ollama pret - GPU 0+1 split%n%n");
            }
        }

        // 2. Lister les modeles — favoris en tete
        System.out.println("Connexion a Ollama (" + ollama.baseUrl() + ")...");
        OllamaAdapter probe = ollama.adapter("_probe");
        List<ModelEntry> allModels;
        try {
            allModels = probe.listModels(Long.MAX_VALUE);
        } catch (Exception e) {
            System.err.println("ERREUR : impossible de contacter Ollama : " + e.getMessage());
            System.exit(1);
            return;
        }
        if (allModels.isEmpty()) {
            System.err.println("ERREUR : aucun modele disponible dans Ollama.");
            System.exit(1);
            return;
        }

        // Trier : favoris en tete dans l'ordre des proprietes, reste apres
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

        // 3. Lister les scenarios
        System.out.println();
        var htmlExport      = new HtmlFileExportAdapter(fileLog::runDir);
        var beatsConfig     = buildBeatsConfig(props);
        var correctorConfig = buildCorrectorConfig(props);
        OllamaAdapter llm = ollama.adapter(selectedModel, log);
        var service = RedacteurModule.assemble(
            llm, new ScenarioFileAdapter(), log, htmlExport, beatsConfig, correctorConfig);
        var scenarios = service.listScenarios(scenarioRoot);
        if (scenarios.isEmpty()) {
            System.err.println("ERREUR : aucun scenario dans '" + scenarioRoot + "'.");
            System.err.println("Un scenario doit etre un sous-repertoire contenant scenario.md ou scenario.yaml.");
            System.exit(1);
            return;
        }

        System.out.println("Scenarios disponibles :");
        for (int i = 0; i < scenarios.size(); i++) {
            System.out.printf("  %2d. %s%n", i + 1, scenarios.get(i).getFileName());
        }
        System.out.print("Choix du scenario [1] : ");
        String scenInput = scanner.nextLine().trim();
        int scenIdx = scenInput.isBlank() ? 0 : (Integer.parseInt(scenInput) - 1);
        if (scenIdx < 0 || scenIdx >= scenarios.size()) {
            System.err.println("ERREUR : choix invalide.");
            System.exit(1);
        }
        Path selectedScenario = scenarios.get(scenIdx);
        System.out.println(">> Scenario : " + selectedScenario.getFileName());
        fileLog.setOutputDir(selectedScenario.resolve("generated"));

        // 4. Validation
        var errors = service.validate(selectedScenario);
        if (!errors.isEmpty()) {
            System.err.println();
            System.err.println("Erreurs de validation du scenario :");
            errors.forEach(e -> System.err.println("  - " + e.message()));
            System.exit(1);
            return;
        }

        // 5. Profil de generation
        System.out.println();
        System.out.println("Profil de generation :");
        System.out.println("  1. BROUILLON — plan + redaction, agents minimum          (rapide)");
        System.out.println("  2. SIMPLE    — plan critique + redaction critique         (qualite)");
        System.out.println("  3. FULL      — SIMPLE + evaluation globale, plus de retry (complet)");
        System.out.print("Choix [1] : ");
        String profInput = scanner.nextLine().trim();
        GenerationConfig config = switch (profInput) {
            case "2"  -> GenerationConfig.simple();
            case "3"  -> GenerationConfig.full();
            default   -> GenerationConfig.brouillon();
        };
        System.out.println(">> Profil : " + config.qualityLevel()
            + " (jsonMode=" + config.jsonMode() + ")");

        // 6. Confirmation
        System.out.println();
        System.out.println("-----------------------------------");
        System.out.printf("  Modele   : %s%n", selectedModel);
        System.out.printf("  Scenario : %s%n", selectedScenario.getFileName());
        System.out.printf("  Profil   : %s%n", config.qualityLevel());
        System.out.println("-----------------------------------");
        System.out.print("Lancer la generation ? [O/n] : ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("n") || confirm.equalsIgnoreCase("non")) {
            System.out.println("Annule.");
            return;
        }

        // -- 7. Chargement du modele + info materiel
        System.out.print("Chargement du modele... ");
        try {
            llm.probe();
            System.out.println("OK");
        } catch (Exception e) {
            System.out.println("(echec probe : " + e.getMessage() + ")");
        }
        OllamaPsInfo ps = OllamaPsInfo.query(ollama.baseUrl(), selectedModel).orElse(null);
        // Recapture apres chargement — le snapshot "gpus" du menu GPU (etape 1) date d'avant
        // le probe et ne reflete plus la VRAM reellement occupee par le modele charge.
        List<NvidiaSmiSnapshot.GpuStat> gpusAfterLoad = NvidiaSmiSnapshot.capture();
        ModelHardwareDisplay.print("[redacteur] ", ps, gpusAfterLoad);

        // 8. Generation
        System.out.println();
        Instant start = Instant.now();
        Story story;
        try {
            story = service.generate(selectedScenario, config);
        } catch (Exception e) {
            System.err.println();
            System.err.println("ERREUR durant la generation : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        // 9. Export
        Path     outputDir = selectedScenario.resolve("generated");
        Path     fullFile  = new StoryExporter().export(story, outputDir);
        Duration elapsed   = Duration.between(start, Instant.now());

        System.out.println();
        System.out.println("=================================================");
        System.out.println("  Generation terminee !");
        System.out.printf("  Duree    : %d min %02d s%n",
            elapsed.toMinutes(), elapsed.toSecondsPart());
        System.out.printf("  Chapitres: %d%n", story.chapters().size());
        System.out.printf("  Dossier  : %s%n", outputDir.toAbsolutePath());
        System.out.printf("  Histoire : %s%n", fullFile.getFileName());
        if (fileLog.runDir() != null) {
            System.out.printf("  Logs     : %s%n", fileLog.runDir().toAbsolutePath());
        }
        System.out.println("=================================================");
    }

    private static Path chooseScenarioRoot(Properties props, Scanner scanner) {
        String defaultDir  = props.getProperty("scenario.root.default", "");
        String projectDir  = Paths.get("scenarios").toAbsolutePath().toString();

        System.out.println("Ou se trouvent les scenarios ?");
        System.out.println("  ENTREE - Repertoire par defaut : " + defaultDir);
        System.out.println("  1      - Repertoire du projet  : " + projectDir);
        System.out.println("  2      - Autre repertoire...");
        System.out.println();

        while (true) {
            System.out.print("Choix [ENTREE/1/2] : ");
            String choice = scanner.nextLine().trim();
            Path root = switch (choice) {
                case ""  -> Paths.get(defaultDir);
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
            if (!root.toFile().isDirectory()) {
                System.err.println("ERREUR : le repertoire '" + root + "' n'existe pas.");
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
        try (InputStream is = RedacteurCli.class.getResourceAsStream("/redacteur.properties")) {
            if (is != null) props.load(is);
        } catch (IOException ignored) {}
        return props;
    }

    private static List<String> loadFavoris(Properties props) {
        List<String> result = new ArrayList<>();
        for (int i = 1; ; i++) {
            String val = props.getProperty("favoris." + i);
            if (val == null || val.isBlank()) break;
            result.add(val.trim());
        }
        return result;
    }

    private static OllamaConfig buildOllamaConfig(Properties props) {
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

    private static BeatsConfig buildBeatsConfig(Properties props) {
        int base      = Integer.parseInt(props.getProperty("beats.base",            "2"));
        int ratio     = Integer.parseInt(props.getProperty("beats.per.words.ratio", "75"));
        int tolerance = Integer.parseInt(props.getProperty("beats.tolerance.pct",   "20"));
        return new BeatsConfig(base, ratio, tolerance);
    }

    private static CorrectorConfig buildCorrectorConfig(Properties props) {
        float threshold      = Float.parseFloat(props.getProperty("corrector.repeat.threshold.per.word", "0.010"));
        int   minCorrections = Integer.parseInt( props.getProperty("corrector.repeat.min.corrections",   "7"));
        int   maxPasses      = Integer.parseInt( props.getProperty("corrector.repeat.max.passes",        "3"));
        return new CorrectorConfig(threshold, minCorrections, maxPasses);
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
