package storymagine.testllm.coeur.domaine;

import org.junit.jupiter.api.Test;
import storymagine.commun.coeur.ports.LlmCallContext;
import storymagine.commun.coeur.ports.LlmResult;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.ModelEntry;
import storymagine.commun.coeur.ports.ModelLifecyclePort;
import storymagine.testllm.coeur.domaine.BenchmarkRunner.BenchEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkRunnerTest {

    // ---- stubs ----

    private static BenchmarkRunner.AdapterFactory factoryWith(ModelCallPort call) {
        return factoryWith(call, () -> {});
    }

    private static BenchmarkRunner.AdapterFactory factoryWith(ModelCallPort call, Runnable probeAction) {
        return new BenchmarkRunner.AdapterFactory() {
            @Override public ModelCallPort callPort(String m, int ctx, long sz, boolean think) { return call; }
            @Override public ModelLifecyclePort lifecyclePort(String m, int ctx, long sz)      { return lifecycleStub(probeAction); }
            @Override public int detectContext(String m)                                        { return 4096; }
            @Override public boolean detectThinking(String m)                                  { return false; }
            @Override public LongSupplier vramSampler(String m)                                { return () -> 512L; }
            @Override public Supplier<HardwareUsage> hardwareSampler()                         { return () -> HardwareUsage.ABSENT; }
            @Override public int configuredCtx()                                               { return 4096; }
            @Override public boolean isLargeModel(long sz)                                     { return false; }
        };
    }

    private static ModelLifecyclePort lifecycleStub(Runnable probeAction) {
        return new ModelLifecyclePort() {
            @Override public void probe()                                { probeAction.run(); }
            @Override public void unload()                              {}
            @Override public String modelName()                         { return "stub"; }
            @Override public int contextWindow()                        { return 4096; }
            @Override public String modelParamSize()                    { return ""; }
            @Override public String modelQuantization()                 { return ""; }
            @Override public String modelFamily()                       { return ""; }
            @Override public String modelInfoBlock()                    { return ""; }
            @Override public boolean supportsThinking()                 { return false; }
            @Override public List<ModelEntry> listModels(long m)        { return List.of(); }
            @Override public int detectContext(String model)            { return 4096; }
        };
    }

    /** Retourne des textes de longueurs données en séquence (la dernière est répétée si dépassée). */
    private static ModelCallPort callPortTexts(int... lengths) {
        AtomicInteger idx = new AtomicInteger(0);
        return new ModelCallPort() {
            @Override public LlmResult generate(String s, String u, double t, LlmCallContext ctx) {
                int len = lengths[Math.min(idx.getAndIncrement(), lengths.length - 1)];
                return new LlmResult("x".repeat(len), 0, 0, 0L);
            }
            @Override public int contextWindow() { return 4096; }
        };
    }

    private static PasseBench passeTest() {
        return new PasseBench("test", "sys", "usr");
    }

    private static List<BenchEvent> run(BenchmarkRunner runner) {
        List<BenchEvent> events = new ArrayList<>();
        runner.run(List.of(new ModelEntry("m", 0L)), List.of(passeTest()), events::add);
        return events;
    }

    // ---- tests divergence (detectDivergence est privée → testée via les events) ----

    @Test
    void pasDesDivergenceQuandTokensIdentiques() {
        // mean=100, aucun écart → pas de Divergence
        BenchmarkRunner runner = new BenchmarkRunner(factoryWith(callPortTexts(100, 100, 100)), 3, 0, 0L);

        assertFalse(run(runner).stream().anyMatch(e -> e instanceof BenchEvent.Divergence));
    }

    @Test
    void divergenceDetecteeSiUnRunDepasseSeuilDeCinquantePourcent() {
        // [50, 200, 50] → mean=100, |200-100|/100 = 100% > 50% → Divergence
        BenchmarkRunner runner = new BenchmarkRunner(factoryWith(callPortTexts(50, 200, 50)), 3, 0, 0L);

        assertTrue(run(runner).stream().anyMatch(e -> e instanceof BenchEvent.Divergence));
    }

    @Test
    void unSeulRunNeDeclenchePasDivergence() {
        // size < 2 → la méthode retourne false directement
        BenchmarkRunner runner = new BenchmarkRunner(factoryWith(callPortTexts(100)), 1, 0, 0L);

        assertFalse(run(runner).stream().anyMatch(e -> e instanceof BenchEvent.Divergence));
    }

    // ---- test probe fail ----

    @Test
    void probeFailSkipLeModele() {
        Runnable probeFail = () -> { throw new RuntimeException("connexion KO"); };
        BenchmarkRunner runner = new BenchmarkRunner(factoryWith(callPortTexts(100), probeFail), 3, 0, 0L);
        List<BenchEvent> events = run(runner);

        assertTrue(events.stream().anyMatch(e -> e instanceof BenchEvent.ProbeFail));
        assertFalse(events.stream().anyMatch(e -> e instanceof BenchEvent.RunOk));
        assertFalse(events.stream().anyMatch(e -> e instanceof BenchEvent.PasseComplete));
    }

    // ---- test erreurs partielles dans runPasse ----

    @Test
    void erreurPartielleNAccumuleQueLesSuccesDansElapsed() {
        // run 1 OK, run 2 FAIL, run 3 OK → elapsed.size()=2, texts.size()=3
        AtomicInteger call = new AtomicInteger(0);
        ModelCallPort port = new ModelCallPort() {
            @Override public LlmResult generate(String s, String u, double t, LlmCallContext ctx) {
                if (call.getAndIncrement() == 1) throw new RuntimeException("timeout");
                return new LlmResult("x".repeat(100), 0, 0, 0L);
            }
            @Override public int contextWindow() { return 4096; }
        };
        BenchmarkRunner runner = new BenchmarkRunner(factoryWith(port), 3, 0, 0L);
        List<BenchEvent> events = run(runner);

        BenchEvent.PasseComplete pc = events.stream()
                .filter(e -> e instanceof BenchEvent.PasseComplete)
                .map(e -> (BenchEvent.PasseComplete) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("PasseComplete manquant"));

        assertEquals(2, pc.elapsed().size(), "Seuls les 2 runs OK contribuent a elapsed");
        assertEquals(3, pc.texts().size(),   "Les 3 tentatives produisent un texte (dont 1 ERREUR)");
        assertTrue(pc.texts().get(1).startsWith("ERREUR"), "Le texte du run echoue commence par ERREUR");
    }
}
