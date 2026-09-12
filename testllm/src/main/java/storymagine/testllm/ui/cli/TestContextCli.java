package storymagine.testllm.ui.cli;

import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaConfig;
import storymagine.commun.infra.OllamaEnvDisplay;
import storymagine.commun.infra.OllamaLauncher;
import storymagine.commun.infra.OllamaPsInfo;
import storymagine.testllm.coeur.domaine.ContextSizes;
import storymagine.testllm.coeur.domaine.PasseBench;
import storymagine.testllm.coeur.domaine.Passes;
import storymagine.testllm.infra.ContextCsvWriter;
import storymagine.testllm.infra.TestLlmConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Diagnostic ponctuel : mesure l'effet de num_ctx sur la vitesse de generation et la repartition
 * CPU/GPU du modele (via /api/ps — seule mesure fiable ici, propre au processus Ollama ; pas
 * d'utilisation CPU/GPU systeme type nvidia-smi/typeperf, qui capterait n'importe quelle autre
 * activite de la machine), pour UN modele choisi interactivement. Balaye les paliers
 * 16k/32k/64k/96k/128k/192k/256k (bornes par le contexte natif du modele, voir ContextSizes),
 * toujours sans thinking — le but est l'impact du contexte, pas la comparaison thinking/pas-thinking
 * (deja couverte par testLLM).
 */
public class TestContextCli {

    private static final int RUNS_DEFAUT = 3;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        OllamaEnvDisplay.print("[testContext] ");

        TestLlmConfig config    = TestLlmConfig.load(Paths.get("testllm", "testllm.properties"));
        OllamaConfig  ollama    = config.ollamaConfig();
        String        ollamaUrl = ollama.baseUrl();

        String ts         = timestamp();
        Path   rootDir    = Paths.get(config.outputDir());
        Path   runDir     = rootDir.resolve("context-" + ts);
        Files.createDirectories(runDir);
        String benchDate  = ts.substring(0, 4) + "-" + ts.substring(4, 6) + "-" + ts.substring(6, 8);
        String benchHeure = ts.substring(9, 11) + ":" + ts.substring(11, 13);
        Path   csvFile    = rootDir.resolve("resultats-context.csv");

        System.out.printf("[testContext] Repertoire : %s%n", runDir.toAbsolutePath());

        String modele;
        int    runs;
        String gpuModeLabel;

        try (Scanner sc = new Scanner(System.in)) {
            List<NvidiaSmiSnapshot.GpuStat> gpus = NvidiaSmiSnapshot.capture(false);

            System.out.println("Configuration GPU Ollama :");
            System.out.printf("  1. Utiliser Ollama deja demarre%n");
            printGpuOption(2, gpus, 0);
            printGpuOption(3, gpus, 1);
            System.out.printf("  4. Relancer sur GPU 0+1 - split auto%n");

            int gpuChoice = 0;
            while (gpuChoice < 1 || gpuChoice > 4) {
                System.out.print("Choix GPU [1-4] : ");
                String line = sc.nextLine().trim();
                try { gpuChoice = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
                if (gpuChoice < 1 || gpuChoice > 4) System.out.println("  Entree invalide, choisir 1-4.");
            }
            System.out.println();

            runs = demanderNombreBoucles(sc, RUNS_DEFAUT);

            gpuModeLabel = resolveGpuMode(gpuChoice, gpus, ollamaUrl);

            modele = choisirModele(config, sc);
        }

        OllamaAdapter    probe      = ollama.adapter(modele, ollama.contextWindowSize(), 0L, false);
        List<ModelEntry> tousModels = probe.listModels(Long.MAX_VALUE);
        long sizeBytes = tousModels.stream()
                .filter(m -> m.name().equals(modele)).findFirst()
                .map(ModelEntry::sizeBytes).orElse(0L);
        int nativeMax = probe.detectContext(modele);
        List<Integer> ctxSizes = ContextSizes.pourModele(nativeMax);

        System.out.printf("%n[testContext] Modele : %s%n", modele);
        System.out.printf("[testContext] Contexte natif detecte : %s%n",
                nativeMax > 0 ? (nativeMax / 1024) + "k" : "inconnu");
        System.out.printf("[testContext] Paliers a tester : %s%n",
                ctxSizes.stream().map(c -> (c / 1024) + "k").reduce((a, b) -> a + ", " + b).orElse(""));

        // A partir d'ici, tout ce qui est imprime est aussi capture pour resume.txt.
        ByteArrayOutputStream capture  = new ByteArrayOutputStream();
        PrintStream           original = System.out;
        System.setOut(new PrintStream(new TeeOutputStream(original, capture), true, StandardCharsets.UTF_8));

        List<ContextCsvWriter.Ligne> lignes = new ArrayList<>();
        OllamaAdapter dernierAdapter = null;

        try {
            for (int ctx : ctxSizes) {
                System.out.printf("%n[testContext] --- Contexte %dk (num_ctx=%d) ---%n", ctx / 1024, ctx);

                OllamaAdapter llm = ollama.adapter(modele, ctx, sizeBytes, false);
                dernierAdapter = llm;

                System.out.printf("[testContext] Chargement du modele...%n");
                long probeStart = System.currentTimeMillis();
                try {
                    llm.probe();
                    System.out.printf("[testContext] Modele charge (%dms).%n",
                            System.currentTimeMillis() - probeStart);
                } catch (Exception e) {
                    System.out.printf("[testContext] ECHEC chargement a %dk : %s -> arret du balayage.%n",
                            ctx / 1024, e.getMessage());
                    lignes.add(new ContextCsvWriter.Ligne(ctx, 0, "FAIL", 0, ""));
                    break;
                }

                OllamaPsInfo ps = OllamaPsInfo.query(ollamaUrl, modele).orElse(null);
                int ctxVerifie = ps != null ? ps.contextLength() : 0;
                if (ctxVerifie == ctx) {
                    System.out.printf("[testContext] ollama ps -> contexte verifie : %dk (OK)%n", ctxVerifie / 1024);
                } else {
                    System.out.printf("[testContext] ollama ps -> contexte annonce : %s (demande : %dk)%n",
                            ctxVerifie > 0 ? (ctxVerifie / 1024) + "k" : "inconnu", ctx / 1024);
                }
                String processorLabel = ps != null ? ps.processorLabel() : "?";

                double sumTps = 0;
                int    cntOk  = 0;
                for (PasseBench passe : Passes.TOUTES) {
                    for (int i = 1; i <= runs; i++) {
                        try {
                            LlmResult r = llm.generate(passe.systemPrompt(), passe.userPrompt(), 0.7);
                            System.out.printf("[testContext] |  %-6s run %d/%d -> tok/s : %-6.2f%n",
                                    passe.nom(), i, runs, r.tokensPerSecond());
                            sumTps += r.tokensPerSecond();
                            cntOk++;
                        } catch (Exception e) {
                            System.out.printf("[testContext] |  %-6s run %d/%d -> FAIL : %s%n",
                                    passe.nom(), i, runs, e.getMessage());
                        }
                    }
                }

                double tokSMoy = cntOk > 0 ? sumTps / cntOk : 0;
                String statut  = cntOk > 0 ? "OK" : "FAIL";
                System.out.printf("[testContext] Resultat %dk -> tok/s moy : %s  |  %s%n",
                        ctx / 1024, cntOk > 0 ? String.format("%.2f", tokSMoy) : "FAIL", processorLabel);

                lignes.add(new ContextCsvWriter.Ligne(ctx, ctxVerifie, statut, tokSMoy, processorLabel));

                if (cntOk == 0) {
                    System.out.printf("[testContext] Aucune generation reussie a %dk -> arret du balayage.%n", ctx / 1024);
                    break;
                }
            }

            System.out.println();
            System.out.printf("Modele : %s%n%n", modele);
            System.out.printf("%-10s %10s %10s %20s%n",
                    "Contexte", "tok/s", "statut", "CPU/GPU (ollama ps)");
            for (ContextCsvWriter.Ligne l : lignes) {
                System.out.printf("%-10s %10s %10s %20s%n",
                        (l.ctxDemande() / 1024) + "k",
                        l.tokSMoy() > 0 ? String.format("%.2f", l.tokSMoy()) : "-",
                        l.statut(),
                        l.ollamaProcessor().isEmpty() ? "-" : l.ollamaProcessor());
            }

            new ContextCsvWriter().appendResults(csvFile, benchDate, benchHeure, modele, gpuModeLabel, runs, lignes);

        } finally {
            if (dernierAdapter != null) {
                System.out.printf("%n[testContext] Dechargement du modele...%n");
                dernierAdapter.unload();
            }
            Path resumeFile = runDir.resolve("resume.txt");
            Files.writeString(resumeFile, capture.toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            System.setOut(original);
            System.out.printf("%n[testContext] Resume : %s%n", resumeFile.toAbsolutePath());
            System.out.printf("[testContext] CSV    : %s%n", csvFile.toAbsolutePath());
        }
    }

    /** Duplique chaque octet ecrit vers deux flux — utilise pour capturer la console dans resume.txt. */
    private static class TeeOutputStream extends OutputStream {
        private final OutputStream a, b;
        TeeOutputStream(OutputStream a, OutputStream b) { this.a = a; this.b = b; }
        @Override public void write(int i) throws IOException { a.write(i); b.write(i); }
        @Override public void write(byte[] buf, int off, int len) throws IOException { a.write(buf, off, len); b.write(buf, off, len); }
        @Override public void flush() throws IOException { a.flush(); b.flush(); }
    }

    private static String timestamp() {
        LocalDateTime n = LocalDateTime.now();
        return String.format("%d%02d%02d-%02d%02d",
                n.getYear(), n.getMonthValue(), n.getDayOfMonth(), n.getHour(), n.getMinute());
    }

    private static int demanderNombreBoucles(Scanner sc, int defaut) {
        while (true) {
            System.out.printf("Nombre de boucles par test [%d] : ", defaut);
            String line = sc.nextLine().trim();
            if (line.isBlank()) return defaut;
            try {
                int n = Integer.parseInt(line);
                if (n >= 1 && n <= 10) return n;
            } catch (NumberFormatException ignored) {}
            System.out.println("  Entree invalide, choisir un nombre entre 1 et 10 (ou Entree pour " + defaut + ").");
        }
    }

    // ── Choix du modele (repris de TestMtpCli.choisirModele — favoris en tete, marques "*") ──

    private static String choisirModele(TestLlmConfig config, Scanner sc) {
        System.out.println("Connexion a Ollama...");
        OllamaAdapter probe = config.ollamaConfig().adapter("_probe");
        List<ModelEntry> allModels = probe.listModels(Long.MAX_VALUE);
        if (allModels.isEmpty()) {
            System.err.println("ERREUR : aucun modele disponible dans Ollama.");
            System.exit(1);
        }

        List<String> favoris = config.favoris();
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
            boolean    isFav  = favoris.contains(m.name());
            String     marker = isFav ? " *" : "  ";
            System.out.printf("%s %2d. %-60s %.1f Go%n", marker, i + 1, m.name(), m.sizeBytes() / 1_073_741_824.0);
        }
        System.out.println("  (* = favori)");

        int choix = -1;
        while (choix < 1 || choix > sorted.size()) {
            System.out.printf("Choix du modele [1-%d] (Entree = 1) : ", sorted.size());
            String line = sc.nextLine().trim();
            if (line.isBlank()) { choix = 1; break; }
            try { choix = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
            if (choix < 1 || choix > sorted.size()) System.out.printf("  Entree invalide, choisir 1-%d.%n", sorted.size());
        }
        return sorted.get(choix - 1).name();
    }

    // ── GPU (repris de TestMtpCli.resolveGpuMode) ──

    private static String resolveGpuMode(int choice, List<NvidiaSmiSnapshot.GpuStat> gpus,
                                        String baseUrl) throws Exception {
        switch (choice) {
            case 1 -> {
                if (!OllamaLauncher.isReachable(baseUrl)) {
                    System.err.println("[testContext] Ollama non joignable. Lancez Ollama d'abord ou choisissez 2-4.");
                    System.exit(1);
                }
                System.out.printf("[testContext] Ollama deja demarre (GPU inconnu).%n%n");
                return "?";
            }
            case 2 -> {
                System.out.printf("[testContext] Relancement Ollama sur GPU 0...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0", baseUrl);
                String label = gpuLabel(gpus, 0);
                System.out.printf("[testContext] Ollama pret - %s%n%n", label);
                return label;
            }
            case 3 -> {
                System.out.printf("[testContext] Relancement Ollama sur GPU 1...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("1", baseUrl);
                String label = gpuLabel(gpus, 1);
                System.out.printf("[testContext] Ollama pret - %s%n%n", label);
                return label;
            }
            default -> { // 4
                System.out.printf("[testContext] Relancement Ollama sur GPU 0+1 (CUDA_VISIBLE_DEVICES=0,1)...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0,1", baseUrl);
                System.out.printf("[testContext] Ollama pret - GPU 0+1 split%n%n");
                return "GPU 0+1 split";
            }
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
}
