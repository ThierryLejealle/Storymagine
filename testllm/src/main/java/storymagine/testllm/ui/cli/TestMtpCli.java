package storymagine.testllm.ui.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaEnvDisplay;
import storymagine.commun.infra.OllamaLauncher;
import storymagine.testllm.coeur.domaine.ThinkMode;
import storymagine.testllm.infra.MtpCsvWriter;
import storymagine.testllm.infra.TestLlmConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Diagnostic ponctuel : mesure l'effet du paramètre Ollama "draft_num_predict" (MTP —
 * Multi-Token Prediction) sur la vitesse de génération, pour un modèle choisi interactivement
 * (favoris en tête, voir storymagine.properties). Un seul appel /api/chat par valeur de MTP
 * testée, pas de passage par OllamaAdapter pour l'appel lui-même (qui ne porte pas d'option
 * arbitraire type draft_num_predict) — seulement pour lister les modèles disponibles.
 */
public class TestMtpCli {

    /**
     * Scénario testé : génération substantielle (~500 mots) pour que draft_num_predict ait un
     * effet mesurable sur le débit — un haïku (~20 tokens) est trop court, domine par le bruit.
     */
    private enum Scenario {
        HISTOIRE("Histoire (aviateur)",
            "You are a writer. Reply only with the story text — no title, no explanation, no commentary. Write in English.",
            "Write a short story of about 500 words, in English, about a pilot during World War II."),
        FONCTION("Fonction Java",
            "You are a Java developer. Reply only with the code — no explanation, no commentary outside the code. " +
            "Use English for all variable names and comments.",
            "Write a Java class LruCache<K,V>: fixed capacity, methods get(K), put(K,V), remove(K) and " +
            "containsKey(K), LRU eviction policy (least recently used discarded first), a toString() method " +
            "listing the keys in LRU order (most recently used first), and a short javadoc for each public " +
            "method. Use English for all names and comments.");

        final String label, systemPrompt, userPrompt;

        Scenario(String label, String systemPrompt, String userPrompt) {
            this.label        = label;
            this.systemPrompt = systemPrompt;
            this.userPrompt   = userPrompt;
        }
    }

    private static final int[] MTP_VALUES = {1, 2, 3, 4, 5, 6};
    private static final int   RUNS_DEFAUT = 3;

    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        OllamaEnvDisplay.print("[testMTP] ");

        TestLlmConfig config    = TestLlmConfig.load(Paths.get("testllm", "testllm.properties"));
        String        ollamaUrl = config.ollamaConfig().baseUrl();

        String ts         = timestamp();
        Path   rootDir    = Paths.get(config.outputDir());
        Path   runDir     = rootDir.resolve("mtp-" + ts);
        Files.createDirectories(runDir);
        String benchDate  = ts.substring(0, 4) + "-" + ts.substring(4, 6) + "-" + ts.substring(6, 8);
        String benchHeure = ts.substring(9, 11) + ":" + ts.substring(11, 13);
        Path   csvFile    = rootDir.resolve("resultats-mtp.csv");

        System.out.printf("[testMTP] Repertoire : %s%n", runDir.toAbsolutePath());

        String          modele;
        int             runs;
        ThinkMode       thinkMode;
        List<Scenario>  scenarios;

        try (Scanner sc = new Scanner(System.in)) {
            List<NvidiaSmiSnapshot.GpuStat> gpus = NvidiaSmiSnapshot.capture(false);

            System.out.println("Configuration GPU Ollama :");
            System.out.printf("  1. Utiliser Ollama déjà démarré%n");
            printGpuOption(2, gpus, 0);
            printGpuOption(3, gpus, 1);
            System.out.printf("  4. Relancer sur GPU 0+1 - split auto%n");

            int gpuChoice = 0;
            while (gpuChoice < 1 || gpuChoice > 4) {
                System.out.print("Choix GPU [1-4] : ");
                String line = sc.nextLine().trim();
                try { gpuChoice = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
                if (gpuChoice < 1 || gpuChoice > 4) System.out.println("  Entrée invalide, choisir 1-4.");
            }
            System.out.println();

            runs = demanderNombreBoucles(sc, RUNS_DEFAUT);

            thinkMode = demanderThinkMode(sc);

            scenarios = demanderScenarios(sc);

            resolveGpuMode(gpuChoice, gpus, ollamaUrl);

            modele = choisirModele(config, sc);
        }

        // testMTP ne detecte pas si le modele supporte reellement le thinking (pas de probe /api/show
        // ici) : le mode choisi est applique tel quel, versions(true) traite juste SANS/AVEC/LES_DEUX.
        List<Boolean> thinkVars = thinkMode.versions(true);

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        // A partir d'ici, tout ce qui est imprime est aussi capture pour resume.txt — la selection
        // interactive precedente (menus GPU/modele) n'a pas besoin d'etre archivee, comme testLLM.
        ByteArrayOutputStream capture  = new ByteArrayOutputStream();
        PrintStream           original = System.out;
        System.setOut(new PrintStream(new TeeOutputStream(original, capture), true, StandardCharsets.UTF_8));

        MtpCsvWriter csvWriter = new MtpCsvWriter();

        // try/finally : resume.txt doit etre ecrit avec ce qui a ete capture jusque-la meme si
        // un plantage (ex. crash Ollama en cours de sweep) interrompt la boucle plus bas.
        try {
            for (Scenario scenario : scenarios) {
                for (boolean think : thinkVars) {
                    System.out.printf("%n[testMTP] Test MTP (draft_num_predict) — modèle : %s — scénario : %s — think:%s — %d boucle(s) par valeur%n%n",
                        modele, scenario.label, think ? "O" : "N", runs);

                    List<Map.Entry<String, ResultatAgrege>> resultats = new ArrayList<>();

                    resultats.add(Map.entry("(aucun)", testerValeur(http, ollamaUrl, modele, null, "(aucun)", runs, think, scenario)));

                    for (int mtp : MTP_VALUES) {
                        String label = String.valueOf(mtp);
                        resultats.add(Map.entry(label, testerValeur(http, ollamaUrl, modele, mtp, label, runs, think, scenario)));
                    }

                    System.out.println();
                    System.out.printf("Modèle : %s — scénario : %s — think:%s%n%n", modele, scenario.label, think ? "O" : "N");
                    System.out.printf("%-10s %10s %10s%n", "MTP", "tokens moy", "tok/s moy");
                    for (Map.Entry<String, ResultatAgrege> r : resultats) {
                        if (r.getValue() == null) {
                            System.out.printf("%-10s %10s %10s%n", r.getKey(), "FAIL", "FAIL");
                        } else {
                            System.out.printf("%-10s %10.0f %10.2f%n", r.getKey(), r.getValue().avgTokens(), r.getValue().avgTokPerSec());
                        }
                    }

                    List<MtpCsvWriter.Ligne> lignes = resultats.stream()
                            .filter(r -> r.getValue() != null)
                            .map(r -> new MtpCsvWriter.Ligne(r.getKey(), r.getValue().avgTokens(), r.getValue().avgTokPerSec()))
                            .toList();
                    csvWriter.appendResults(csvFile, benchDate, benchHeure, modele, scenario.name(), think, runs, lignes);
                }
            }
        } finally {
            Path resumeFile = runDir.resolve("resume.txt");
            Files.writeString(resumeFile, capture.toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            System.setOut(original);
            System.out.printf("%n[testMTP] Resume : %s%n", resumeFile.toAbsolutePath());
            System.out.printf("[testMTP] CSV    : %s%n", csvFile.toAbsolutePath());
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

    /** Résultat d'un appel : tokens générés, débit, et aperçu du texte (pour vérifier qu'un vrai haïku sort). */
    private record Resultat(long tokens, double tokPerSec, String contenu) {}

    /** Moyenne des résultats des N boucles pour une valeur de draft_num_predict donnée. */
    private record ResultatAgrege(double avgTokens, double avgTokPerSec) {}

    /**
     * Boucle "runs" appels pour une valeur de draft_num_predict, une ligne live par run, puis
     * moyenne sur les seuls runs reussis. Un run en echec (ex. crash Ollama) est journalise et
     * saute plutot que d'interrompre tout le sweep — retourne null si tous les runs ont echoue.
     */
    private static ResultatAgrege testerValeur(HttpClient http, String baseUrl, String modele, Integer draftNumPredict,
                                                String label, int runs, boolean think, Scenario scenario) {
        long   sommeTokens = 0;
        double sommeTps    = 0;
        int    succes      = 0;
        for (int i = 1; i <= runs; i++) {
            System.out.printf("[testMTP] |  draft_num_predict : %-8s  run %d/%d -> en cours...%n", label, i, runs);
            try {
                Resultat resultat = appel(http, baseUrl, modele, draftNumPredict, think, scenario);
                System.out.printf("[testMTP] |  draft_num_predict : %-8s  run %d/%d -> tokens : %-4d  tok/s : %-6.2f  texte : %s%n",
                    label, i, runs, resultat.tokens(), resultat.tokPerSec(), apercu(resultat.contenu()));
                sommeTokens += resultat.tokens();
                sommeTps    += resultat.tokPerSec();
                succes++;
            } catch (Exception e) {
                System.out.printf("[testMTP] |  draft_num_predict : %-8s  run %d/%d -> FAIL : %s%n", label, i, runs, e.getMessage());
            }
        }
        if (succes == 0) return null;
        return new ResultatAgrege((double) sommeTokens / succes, sommeTps / succes);
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
            System.out.println("  Entrée invalide, choisir un nombre entre 1 et 10 (ou Entrée pour " + defaut + ").");
        }
    }

    private static ThinkMode demanderThinkMode(Scanner sc) {
        System.out.println("Thinking à tester :");
        System.out.println("  1. Sans thinking");
        System.out.println("  2. Avec thinking");
        System.out.println("  3. Les deux (défaut)");
        while (true) {
            System.out.print("Choix [1-3] (Entrée = 3) : ");
            String line = sc.nextLine().trim();
            if (line.isBlank()) return ThinkMode.LES_DEUX;
            switch (line) {
                case "1": return ThinkMode.SANS;
                case "2": return ThinkMode.AVEC;
                case "3": return ThinkMode.LES_DEUX;
                default:  System.out.println("  Entrée invalide, choisir 1-3.");
            }
        }
    }

    private static List<Scenario> demanderScenarios(Scanner sc) {
        System.out.println("Scénario à tester :");
        System.out.printf("  1. %s%n", Scenario.HISTOIRE.label);
        System.out.printf("  2. %s%n", Scenario.FONCTION.label);
        System.out.println("  3. Les deux (défaut)");
        while (true) {
            System.out.print("Choix [1-3] (Entrée = 3) : ");
            String line = sc.nextLine().trim();
            if (line.isBlank()) return List.of(Scenario.HISTOIRE, Scenario.FONCTION);
            switch (line) {
                case "1": return List.of(Scenario.HISTOIRE);
                case "2": return List.of(Scenario.FONCTION);
                case "3": return List.of(Scenario.HISTOIRE, Scenario.FONCTION);
                default:  System.out.println("  Entrée invalide, choisir 1-3.");
            }
        }
    }

    private static String apercu(String contenu) {
        String s = contenu.replace("\n", " / ").trim();
        return s.length() > 70 ? s.substring(0, 70) + "…" : s;
    }

    /** Un seul appel /api/chat, non-stream. */
    private static Resultat appel(HttpClient http, String baseUrl, String modele, Integer draftNumPredict,
                                   boolean think, Scenario scenario) throws Exception {
        Map<String, Object> options = new LinkedHashMap<>();
        if (draftNumPredict != null) options.put("draft_num_predict", draftNumPredict);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modele);
        body.put("stream", false);
        body.put("think", think); // explicite : sinon la longueur de reflexion varie d'un appel a l'autre et fausse la comparaison MTP
        body.put("options", options);
        body.put("messages", List.of(
            Map.of("role", "system", "content", scenario.systemPrompt),
            Map.of("role", "user",   "content", scenario.userPrompt)
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/chat"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(5))
            .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Ollama HTTP " + response.statusCode() + " : " + response.body());

        JsonNode  root         = JSON.readTree(response.body());
        String    error        = root.path("error").asText("");
        if (!error.isBlank()) throw new RuntimeException("Ollama error : " + error);

        long   evalCount    = root.path("eval_count").asLong(0);
        long   evalDuration = root.path("eval_duration").asLong(0);
        double tokPerSec    = evalDuration > 0 ? evalCount / (evalDuration / 1_000_000_000.0) : 0;
        String contenu      = root.path("message").path("content").asText("");

        return new Resultat(evalCount, tokPerSec, contenu);
    }

    // ── Choix du modèle (repris de OllamaSetupCli.chooseModel — favoris en tête, marqués "*") ──

    private static String choisirModele(TestLlmConfig config, Scanner sc) {
        System.out.println("Connexion à Ollama...");
        OllamaAdapter probe = config.ollamaConfig().adapter("_probe");
        List<ModelEntry> allModels = probe.listModels(Long.MAX_VALUE);
        if (allModels.isEmpty()) {
            System.err.println("ERREUR : aucun modèle disponible dans Ollama.");
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
        System.out.println("Modèles disponibles :");
        for (int i = 0; i < sorted.size(); i++) {
            ModelEntry m      = sorted.get(i);
            boolean    isFav  = favoris.contains(m.name());
            String     marker = isFav ? " *" : "  ";
            System.out.printf("%s %2d. %-60s %.1f Go%n", marker, i + 1, m.name(), m.sizeBytes() / 1_073_741_824.0);
        }
        System.out.println("  (* = favori)");

        int choix = -1;
        while (choix < 1 || choix > sorted.size()) {
            System.out.printf("Choix du modèle [1-%d] (Entrée = 1) : ", sorted.size());
            String line = sc.nextLine().trim();
            if (line.isBlank()) { choix = 1; break; }
            try { choix = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
            if (choix < 1 || choix > sorted.size()) System.out.printf("  Entrée invalide, choisir 1-%d.%n", sorted.size());
        }
        return sorted.get(choix - 1).name();
    }

    // ── GPU (repris de TestLlmCli.resolveGpuMode, option "complet" retirée) ──

    private static void resolveGpuMode(int choice, List<NvidiaSmiSnapshot.GpuStat> gpus,
                                        String baseUrl) throws Exception {
        switch (choice) {
            case 1 -> {
                if (!OllamaLauncher.isReachable(baseUrl)) {
                    System.err.println("[testMTP] Ollama non joignable. Lancez Ollama d'abord ou choisissez 2-4.");
                    System.exit(1);
                }
                System.out.printf("[testMTP] Ollama déjà démarré (GPU inconnu).%n%n");
            }
            case 2 -> {
                System.out.printf("[testMTP] Relancement Ollama sur GPU 0...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0", baseUrl);
                System.out.printf("[testMTP] Ollama prêt - %s%n%n", gpuLabel(gpus, 0));
            }
            case 3 -> {
                System.out.printf("[testMTP] Relancement Ollama sur GPU 1...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("1", baseUrl);
                System.out.printf("[testMTP] Ollama prêt - %s%n%n", gpuLabel(gpus, 1));
            }
            default -> { // 4
                System.out.printf("[testMTP] Relancement Ollama sur GPU 0+1 (CUDA_VISIBLE_DEVICES=0,1)...%n");
                OllamaLauncher.killAll();
                OllamaLauncher.launch("0,1", baseUrl);
                System.out.printf("[testMTP] Ollama prêt - GPU 0+1 split%n%n");
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
