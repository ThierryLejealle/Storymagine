package storymagine.testllm.ui.cli;

import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.testllm.coeur.domaine.BenchRun;
import storymagine.testllm.coeur.service.TestLlmService;
import storymagine.testllm.infra.BenchCsvWriter;
import storymagine.testllm.infra.BenchSession;
import storymagine.testllm.infra.BenchTextFormatter;
import storymagine.testllm.infra.NvidiaSmiSnapshot;
import storymagine.testllm.infra.OllamaLauncher;
import storymagine.testllm.infra.TestLlmAssembler;
import storymagine.testllm.infra.TestLlmConfig;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Point d'entree CLI du module testllm.
 * Usage : TestLlmCli [tout|favoris|choisir]
 */
public class TestLlmCli {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        String mode = args.length > 0 ? args[0].toLowerCase() : "menu";

        Path          propsFile = Paths.get("testllm", "testllm.properties");
        TestLlmConfig config    = TestLlmConfig.load(propsFile);
        String        ollamaUrl = config.ollamaConfig().baseUrl();

        String ts      = timestamp();
        Path   rootDir = Paths.get(config.outputDir());
        Path   runDir  = rootDir.resolve("bench-" + ts);
        Files.createDirectories(runDir);

        String benchDate  = ts.substring(0, 4) + "-" + ts.substring(4, 6) + "-" + ts.substring(6, 8);
        String benchHeure = ts.substring(9, 11) + ":" + ts.substring(11, 13);
        Path   csvFile    = rootDir.resolve("resultats.csv");
        int    runs       = config.runs();

        System.out.printf("[testllm] Repertoire : %s%n", runDir.toAbsolutePath());

        // ── Detection GPU ──────────────────────────────────────────────────────────
        List<NvidiaSmiSnapshot.GpuStat> gpus = NvidiaSmiSnapshot.capture();

        // Variables resolues dans le bloc interactif
        int           gpuChoice        = 0;
        String        resolvedGpuMode  = null;
        String        modeEffectif     = mode;
        String        choixModele      = null;
        BenchSession  interactiveSession = null;
        Path          interactiveDir   = null;
        StringBuilder interactiveResume = null;

        try (Scanner sc = new Scanner(System.in)) {

            // ── Menu GPU ───────────────────────────────────────────────────────────
            System.out.println();
            System.out.println("Configuration GPU Ollama :");
            System.out.printf("  1. Utiliser Ollama deja demarre%n");
            printGpuOption(2, gpus, 0);
            printGpuOption(3, gpus, 1);
            System.out.printf("  4. Relancer sur GPU 0+1 - split auto%n");
            System.out.printf("  5. Complet : tester sur GPU 0, GPU 1 et GPU 0+1%n");

            while (gpuChoice < 1 || gpuChoice > 5) {
                System.out.print("Choix GPU [1-5] : ");
                String line = sc.nextLine().trim();
                try { gpuChoice = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
                if (gpuChoice < 1 || gpuChoice > 5) System.out.println("  Entree invalide, choisir 1-5.");
            }
            System.out.println();

            // ── Preparation GPU ────────────────────────────────────────────────────
            if (gpuChoice == 5) {
                System.out.printf("[testllm] Mode complet (GPU 0 -> GPU 1 -> GPU 0+1)%n");
                System.out.printf("[testllm] Lancement Ollama sur GPU 0 pour selection de modele...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0", ollamaUrl);
                System.out.printf("[testllm] Ollama pret.%n%n");

                interactiveDir    = runDir.resolve("gpu-0");
                interactiveResume = new StringBuilder();
                creerDossiersPasses(interactiveDir);
                interactiveSession = TestLlmAssembler.assemble(config, interactiveDir, runs,
                        interactiveResume, gpuLabel(gpus, 0));
            } else {
                resolvedGpuMode = resolveGpuMode(gpuChoice, gpus, ollamaUrl);
                interactiveDir    = runDir;
                interactiveResume = new StringBuilder();
                creerDossiersPasses(runDir);
                interactiveSession = TestLlmAssembler.assemble(config, runDir, runs,
                        interactiveResume, resolvedGpuMode);
            }

            // ── Selection mode et modele ───────────────────────────────────────────
            if ("menu".equals(mode) || "choisir".equals(mode)) {
                if ("menu".equals(mode)) {
                    System.out.println("Que voulez-vous tester ?");
                    System.out.println("  1. Tous les modeles Ollama");
                    System.out.println("  2. Favoris (configures dans testllm.properties)");
                    System.out.println("  3. Choisir un modele specifique");
                    while (true) {
                        System.out.print("Choix [1-3] : ");
                        String line = sc.nextLine().trim();
                        if ("1".equals(line))      { modeEffectif = "tout";    break; }
                        else if ("2".equals(line)) { modeEffectif = "favoris"; break; }
                        else if ("3".equals(line)) { modeEffectif = "choisir"; break; }
                        else System.out.println("  Entree invalide, choisir 1-3.");
                    }
                }
                if ("choisir".equals(modeEffectif)) {
                    List<ModelEntry> modeles = interactiveSession.service().listerModeles();
                    if (modeles.isEmpty()) {
                        System.err.printf("[testllm] Aucun modele trouve dans Ollama.%n");
                        System.exit(1);
                    }
                    int w = modeles.stream().mapToInt(e -> e.name().length()).max().orElse(10);
                    System.out.println("Modeles disponibles :");
                    for (int i = 0; i < modeles.size(); i++) {
                        ModelEntry e = modeles.get(i);
                        System.out.printf("  %d. %-" + w + "s   %.1f Go%n",
                                i + 1, e.name(), e.sizeBytes() / 1_073_741_824.0);
                    }
                    while (true) {
                        System.out.printf("Modele [1-%d] : ", modeles.size());
                        String line = sc.nextLine().trim();
                        try {
                            int n = Integer.parseInt(line);
                            if (n >= 1 && n <= modeles.size()) { choixModele = modeles.get(n - 1).name(); break; }
                        } catch (NumberFormatException ignored) {}
                        System.out.printf("  Entree invalide, choisir 1-%d.%n", modeles.size());
                    }
                }
                if ("favoris".equals(modeEffectif)) {
                    List<String> favoris = interactiveSession.service().listerFavoris();
                    if (favoris.isEmpty()) {
                        System.err.println("[testllm] Aucun favori configure dans testllm.properties");
                        System.exit(1);
                    }
                    System.out.println("[testllm] Mode : FAVORIS");
                    favoris.forEach(f -> System.out.printf("  - %s%n", f));
                }
            }
        } // Scanner ferme ici — toute la saisie est terminee

        BenchTextFormatter formatter = new BenchTextFormatter();

        // ── Execution ──────────────────────────────────────────────────────────────
        if (gpuChoice == 5) {
            // Mode 5 : pour chaque modele, tester sur GPU 0, GPU 1, GPU 0+1 en sequence
            List<String> modelesToTest;
            if ("tout".equals(modeEffectif)) {
                modelesToTest = interactiveSession.service().listerModeles().stream()
                        .map(ModelEntry::name).toList();
            } else if ("favoris".equals(modeEffectif)) {
                modelesToTest = interactiveSession.service().listerFavoris();
            } else {
                modelesToTest = List.of(choixModele);
            }

            String[][] gpuConfigs = {
                {"0",   gpuLabel(gpus, 0)},
                {"1",   gpuLabel(gpus, 1)},
                {"0,1", "GPU 0+1 split"},
            };

            Map<String, StringBuilder> resumeByGpu = new LinkedHashMap<>();
            for (String[] gc : gpuConfigs) {
                creerDossiersPasses(runDir.resolve("gpu-" + gc[0].replace(",", "-")));
                resumeByGpu.put(gc[0], new StringBuilder());
            }

            boolean ollamaReadyOnGpu0 = true; // deja lance pendant la phase interactive

            for (String modele : modelesToTest) {
                System.out.printf("%n[testllm] =====================================================%n");
                System.out.printf("[testllm] Modele : %s%n", modele);

                for (int gi = 0; gi < gpuConfigs.length; gi++) {
                    String cuda  = gpuConfigs[gi][0];
                    String label = gpuConfigs[gi][1];
                    Path   gpuDir = runDir.resolve("gpu-" + cuda.replace(",", "-"));

                    if (gi == 0 && ollamaReadyOnGpu0) {
                        ollamaReadyOnGpu0 = false;
                        System.out.printf("%n[testllm] --- %s (deja demarre) ---%n", label);
                    } else {
                        System.out.printf("%n[testllm] --- %s (CUDA_VISIBLE_DEVICES=%s) ---%n", label, cuda);
                        System.out.printf("[testllm] Relancement Ollama...%n");
                        OllamaLauncher.killAll();
                        OllamaLauncher.launch(cuda, ollamaUrl);
                        System.out.printf("[testllm] Ollama pret.%n");
                    }

                    StringBuilder gpuResume = resumeByGpu.get(cuda);
                    BenchSession  session   = TestLlmAssembler.assemble(config, gpuDir, runs, gpuResume, label);
                    BenchRun      benchRun  = session.service().benchModele(modele, gpuDir);
                    ecrireResultats(benchRun, formatter, gpuResume);
                    new BenchCsvWriter().appendResults(csvFile, benchDate, benchHeure, benchRun,
                            session.logger().getGpuStates());
                }
            }

            for (String[] gc : gpuConfigs) {
                Path gpuDir = runDir.resolve("gpu-" + gc[0].replace(",", "-"));
                Files.writeString(gpuDir.resolve("resume.txt"), resumeByGpu.get(gc[0]).toString(),
                        StandardCharsets.UTF_8);
                System.out.printf("[testllm] Resume %s : %s%n", gc[1],
                        gpuDir.resolve("resume.txt").toAbsolutePath());
            }

            System.out.printf("%n[testllm] Mode complet termine.%n");
            System.out.printf("[testllm] CSV global : %s%n", csvFile.toAbsolutePath());

        } else {
            // Mode simple (GPU unique)
            System.out.printf("%n[testllm] Mode : %s%n", modeLabel(modeEffectif, choixModele));
            BenchRun benchRun = lancerBench(modeEffectif, choixModele, interactiveSession.service(), interactiveDir);
            ecrireResultats(benchRun, formatter, interactiveResume);

            Path resumeFile = runDir.resolve("resume.txt");
            Files.writeString(resumeFile, interactiveResume.toString(), StandardCharsets.UTF_8);
            System.out.printf("%n[testllm] Resume : %s%n", resumeFile.toAbsolutePath());

            new BenchCsvWriter().appendResults(csvFile, benchDate, benchHeure, benchRun,
                    interactiveSession.logger().getGpuStates());
            System.out.printf("[testllm] CSV    : %s%n", csvFile.toAbsolutePath());
            System.out.printf("[testllm] Termine.%n");
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────────

    private static String resolveGpuMode(int choice, List<NvidiaSmiSnapshot.GpuStat> gpus,
                                         String baseUrl) throws Exception {
        switch (choice) {
            case 1 -> {
                if (!OllamaLauncher.isReachable(baseUrl)) {
                    System.err.println("[testllm] Ollama non joignable. Lancez Ollama d'abord ou choisissez 2-5.");
                    System.exit(1);
                }
                System.out.printf("[testllm] Ollama deja demarre (GPU inconnu).%n%n");
                return "?";
            }
            case 2 -> {
                System.out.printf("[testllm] Relancement Ollama sur GPU 0...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0", baseUrl);
                String label2 = gpuLabel(gpus, 0);
                System.out.printf("[testllm] Ollama pret - %s%n%n", label2);
                return label2;
            }
            case 3 -> {
                System.out.printf("[testllm] Relancement Ollama sur GPU 1...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("1", baseUrl);
                String label3 = gpuLabel(gpus, 1);
                System.out.printf("[testllm] Ollama pret - %s%n%n", label3);
                return label3;
            }
            default -> { // 4
                System.out.printf("[testllm] Relancement Ollama sur GPU 0+1 (CUDA_VISIBLE_DEVICES=0,1)...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0,1", baseUrl);
                System.out.printf("[testllm] Ollama pret - GPU 0+1 split%n%n");
                return "GPU 0+1 split";
            }
        }
    }

    private static BenchRun lancerBench(String mode, String choixModele,
                                        TestLlmService service, Path dir) {
        return switch (mode) {
            case "tout"    -> service.benchTous(dir);
            case "favoris" -> service.benchFavoris(dir);
            case "choisir" -> service.benchModele(choixModele, dir);
            default -> {
                System.err.printf("[testllm] Mode inconnu : '%s'%n", mode);
                System.exit(1);
                yield null;
            }
        };
    }

    private static void ecrireResultats(BenchRun run, BenchTextFormatter fmt, StringBuilder sb) {
        String synthese = fmt.formatSynthese(run.modeles());
        System.out.println(synthese);
        sb.append(synthese).append(System.lineSeparator());

        String parModele = fmt.formatParModele(run);
        System.out.println(parModele);
        sb.append(parModele).append(System.lineSeparator());

        String divergences = fmt.formatDivergences(run.modeles());
        if (!divergences.isEmpty()) {
            System.out.println(divergences);
            sb.append(divergences).append(System.lineSeparator());
        }
    }

    private static void creerDossiersPasses(Path dir) throws Exception {
        for (String passe : new String[]{"haiku", "moyen", "grand"}) {
            Files.createDirectories(dir.resolve(passe));
        }
    }

    private static void printGpuOption(int num, List<NvidiaSmiSnapshot.GpuStat> gpus, int gpuIndex) {
        NvidiaSmiSnapshot.GpuStat g = gpus.stream()
                .filter(x -> x.index() == gpuIndex).findFirst().orElse(null);
        if (g != null) {
            System.out.printf("  %d. Relancer sur GPU %d - %-32s  %.0f Go%n",
                    num, gpuIndex, g.name(), g.totalMb() / 1024.0);
        } else {
            System.out.printf("  %d. Relancer sur GPU %d (CUDA_VISIBLE_DEVICES=%d)%n",
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

    private static String modeLabel(String mode, String modele) {
        return switch (mode) {
            case "tout"    -> "TOUS les modeles Ollama";
            case "favoris" -> "FAVORIS";
            case "choisir" -> "MODELE UNIQUE — " + modele;
            default        -> mode;
        };
    }

    private static String timestamp() {
        LocalDateTime n = LocalDateTime.now();
        return String.format("%d%02d%02d-%02d%02d",
                n.getYear(), n.getMonthValue(), n.getDayOfMonth(), n.getHour(), n.getMinute());
    }
}
