package storymagine.testllm.coeur.service;

import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.coeur.ports.ModelLifecyclePort;
import storymagine.testllm.coeur.domaine.BenchRun;
import storymagine.testllm.coeur.domaine.BenchmarkRunner;
import storymagine.testllm.coeur.domaine.Passes;

import java.nio.file.Path;
import java.util.List;

/**
 * Implémentation du service de test LLM.
 */
public class TestLlmServiceImpl implements TestLlmService {

    private final ModelLifecyclePort       lifecycle;
    private final BenchmarkRunner          runner;
    private final List<String>             favoris;
    private final BenchmarkRunner.RunLogger logger;

    public TestLlmServiceImpl(ModelLifecyclePort lifecycle,
                               BenchmarkRunner runner,
                               List<String> favoris,
                               BenchmarkRunner.RunLogger logger) {
        this.lifecycle = lifecycle;
        this.runner    = runner;
        this.favoris   = favoris;
        this.logger    = logger;
    }

    @Override
    public BenchRun benchTous(Path outputDir) {
        List<ModelEntry> modeles = lifecycle.listModels(0L);
        return runner.run(modeles, Passes.TOUTES, logger);
    }

    @Override
    public BenchRun benchFavoris(Path outputDir) {
        List<ModelEntry> tous = lifecycle.listModels(0L);
        List<ModelEntry> selectionnes = tous.stream()
                .filter(e -> favoris.contains(e.name()))
                .toList();
        // Les favoris absents d'Ollama sont inclus avec taille 0 pour avertir
        List<String> trouves = selectionnes.stream().map(ModelEntry::name).toList();
        List<ModelEntry> manquants = favoris.stream()
                .filter(f -> !trouves.contains(f))
                .map(f -> new ModelEntry(f, 0L))
                .toList();
        List<ModelEntry> complet = new java.util.ArrayList<>(selectionnes);
        complet.addAll(manquants);
        return runner.runDirect(complet, Passes.TOUTES, logger);
    }

    @Override
    public BenchRun benchModele(String modele, Path outputDir) {
        List<ModelEntry> tous = lifecycle.listModels(0L);
        ModelEntry entry = tous.stream()
                .filter(e -> e.name().equals(modele))
                .findFirst()
                .orElse(new ModelEntry(modele, 0L));
        return runner.run(List.of(entry), Passes.TOUTES, logger);
    }

    @Override
    public List<ModelEntry> listerModeles() {
        return lifecycle.listModels(0L);
    }

    @Override
    public List<String> listerFavoris() {
        return List.copyOf(favoris);
    }
}
