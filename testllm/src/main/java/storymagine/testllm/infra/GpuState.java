package storymagine.testllm.infra;

import java.util.List;

/**
 * Etat GPU capture apres le probe d'un modele (nvidia-smi + ollama /api/ps).
 */
public record GpuState(
        String                        gpuMode,
        OllamaPsInfo                  ps,
        List<NvidiaSmiSnapshot.GpuStat> gpuStats) {

    public static final GpuState ABSENT = new GpuState("?", null, List.of());
}
