package storymagine.testllm.infra;

import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.coeur.ports.ModelLifecyclePort;
import storymagine.commun.infra.OllamaConfig;
import storymagine.testllm.coeur.domaine.BenchmarkRunner;
import storymagine.testllm.coeur.service.TestLlmService;
import storymagine.testllm.coeur.service.TestLlmServiceImpl;

import storymagine.testllm.coeur.domaine.HardwareUsage;

import java.nio.file.Path;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Assemble le coeur testllm avec ses adaptateurs infra.
 */
public class TestLlmAssembler {

    public static BenchSession assemble(TestLlmConfig config, Path runDir,
                                        int runs, StringBuilder resume, String gpuMode) {
        OllamaConfig ollama  = config.ollamaConfig();
        String       baseUrl = ollama.baseUrl();

        BenchmarkRunner.AdapterFactory factory = new BenchmarkRunner.AdapterFactory() {
            @Override
            public ModelCallPort callPort(String model, int ctx, long sizeBytes, boolean think) {
                return ollama.adapter(model, ctx, sizeBytes, think);
            }

            @Override
            public ModelLifecyclePort lifecyclePort(String model, int ctx, long sizeBytes) {
                return ollama.adapter(model, ctx, sizeBytes, false);
            }

            @Override
            public int detectContext(String model) {
                return lifecyclePort(model, ollama.contextWindowSize(), 0L).detectContext(model);
            }

            @Override
            public boolean detectThinking(String model) {
                return ThinkingDetector.detect(ollama.baseUrl(), model);
            }

            @Override
            public LongSupplier vramSampler(String model) {
                return VramPoller.forModel(ollama.baseUrl(), model);
            }

            @Override
            public Supplier<HardwareUsage> hardwareSampler() {
                return HardwarePoller.create();
            }

            @Override
            public int configuredCtx() {
                return ollama.contextWindowSize();
            }

            @Override
            public boolean isLargeModel(long sizeBytes) {
                return ollama.isLargeModel(sizeBytes);
            }
        };

        long maxModelBytes = config.maxModelSizeBytes();
        BenchmarkRunner runner = new BenchmarkRunner(factory, runs, 0, maxModelBytes);

        BenchTextFormatter formatter = new BenchTextFormatter();
        BenchLogWriter     logWriter = new BenchLogWriter();
        ConsoleRunLogger   logger    = new ConsoleRunLogger(formatter, logWriter, runDir, runs, resume,
                                                           gpuMode, baseUrl);

        ModelLifecyclePort lifecycle = ollama.adapter(
                "placeholder", ollama.contextWindowSize(), 0L, false);

        TestLlmService service = new TestLlmServiceImpl(lifecycle, runner, config.favoris(), logger);
        return new BenchSession(service, logger);
    }
}
