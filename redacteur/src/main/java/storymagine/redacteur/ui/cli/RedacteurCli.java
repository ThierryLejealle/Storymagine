package storymagine.redacteur.ui.cli;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.FileLogAdapter;
import storymagine.commun.infra.ModelHardwareDisplay;
import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaPsInfo;
import storymagine.commun.infra.TeeLogAdapter;
import storymagine.commun.ui.cli.OllamaSetupCli;
import storymagine.redacteur.RedacteurModule;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.BeatsConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.write.CorrectorConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.infra.HtmlFileExportAdapter;
import storymagine.redacteur.infra.StoryExporter;
import storymagine.redacteur.infra.checkpoint.JsonCheckpointAdapter;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
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

        FileLogAdapter fileLog = new FileLogAdapter();
        LogPort        log     = new TeeLogAdapter(new ConsoleLogAdapter(), fileLog);

        // 1-2. Menu GPU + choix du modele (flux partage, cf. commun/ui/cli/OllamaSetupCli)
        OllamaSetupCli.Selection setup         = OllamaSetupCli.run(props, scanner, log, "redacteur");
        OllamaAdapter            llm           = setup.llm();
        String                   selectedModel = setup.modelName();

        // 3. Lister les scenarios
        System.out.println();
        var htmlExport      = new HtmlFileExportAdapter(fileLog::runDir);
        var checkpointPort  = new JsonCheckpointAdapter(fileLog::runDir);
        var beatsConfig     = buildBeatsConfig(props);
        var correctorConfig = buildCorrectorConfig(props);
        var service = RedacteurModule.assemble(
            llm, new ScenarioFileAdapter(), log, htmlExport, checkpointPort, beatsConfig, correctorConfig);
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

        // 3bis. Generation(s) interrompue(s) detectee(s) pour ce scenario -> nouvelle ou reprise ?
        Path       generatedDir = selectedScenario.resolve("generated");
        List<Path> resumable    = checkpointPort.findIncomplete(generatedDir);
        Path       resumeRunDir = null;
        if (!resumable.isEmpty()) {
            System.out.println();
            System.out.println("Generation(s) interrompue(s) detectee(s) :");
            for (int i = 0; i < resumable.size(); i++) {
                System.out.printf("  %2d. %s%n", i + 1, resumable.get(i).getFileName());
            }
            System.out.println("  ENTREE - Nouvelle generation");
            System.out.print("Reprendre laquelle ? [ENTREE/1.." + resumable.size() + "] : ");
            String resumeInput = scanner.nextLine().trim();
            if (!resumeInput.isBlank()) {
                int resumeIdx = Integer.parseInt(resumeInput) - 1;
                if (resumeIdx >= 0 && resumeIdx < resumable.size()) {
                    resumeRunDir = resumable.get(resumeIdx);
                }
            }
        }
        boolean isResume = resumeRunDir != null;
        if (isResume) {
            fileLog.resumeRunDir(resumeRunDir);
        } else {
            fileLog.setOutputDir(generatedDir);
        }

        // 4. Validation
        var errors = service.validate(selectedScenario);
        if (!errors.isEmpty()) {
            System.err.println();
            System.err.println("Erreurs de validation du scenario :");
            errors.forEach(e -> System.err.println("  - " + e.message()));
            System.exit(1);
            return;
        }

        // 5. Profil de generation — repris automatiquement depuis le checkpoint en cas de reprise
        GenerationConfig config = null;
        if (!isResume) {
            System.out.println();
            System.out.println("Profil de generation :");
            System.out.println("  1. BROUILLON — plan + redaction, agents minimum          (rapide)");
            System.out.println("  2. SIMPLE    — plan critique + redaction critique         (qualite)");
            System.out.println("  3. FULL      — SIMPLE + evaluation globale, plus de retry (complet)");
            System.out.print("Choix [1] : ");
            String profInput = scanner.nextLine().trim();
            config = switch (profInput) {
                case "2"  -> GenerationConfig.simple();
                case "3"  -> GenerationConfig.full();
                default   -> GenerationConfig.brouillon();
            };
            System.out.println(">> Profil : " + config.qualityLevel()
                + " (jsonMode=" + config.jsonMode() + ")");
        }

        // 6. Confirmation
        System.out.println();
        System.out.println("-----------------------------------");
        System.out.printf("  Modele   : %s%n", selectedModel);
        System.out.printf("  Scenario : %s%n", selectedScenario.getFileName());
        if (isResume) {
            System.out.printf("  Mode     : reprise (%s)%n", resumeRunDir.getFileName());
        } else {
            System.out.printf("  Profil   : %s%n", config.qualityLevel());
        }
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
        OllamaPsInfo ps = OllamaPsInfo.query(setup.ollamaConfig().baseUrl(), selectedModel).orElse(null);
        // Recapture apres chargement — le snapshot "gpus" du menu GPU (etape 1) date d'avant
        // le probe et ne reflete plus la VRAM reellement occupee par le modele charge.
        List<NvidiaSmiSnapshot.GpuStat> gpusAfterLoad = NvidiaSmiSnapshot.capture();
        ModelHardwareDisplay.print("[redacteur] ", ps, gpusAfterLoad);

        // 8. Generation
        System.out.println();
        Instant start = Instant.now();
        Story story;
        try {
            story = isResume ? service.resume(selectedScenario, resumeRunDir)
                              : service.generate(selectedScenario, config);
        } catch (Exception e) {
            System.err.println();
            System.err.println("ERREUR durant la generation : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
            return;
        }

        // 9. Export
        Path     outputDir = generatedDir;
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
}
