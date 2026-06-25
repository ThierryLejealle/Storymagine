package storymagine.commun.infra;

import java.util.List;

/**
 * Affiche les lignes d'info RAM/VRAM et GPU (ollama ps + nvidia-smi) sur stdout.
 * Factorisation partagée par testllm et redacteur.
 */
public class ModelHardwareDisplay {

    /**
     * @param prefix  prefixe de chaque ligne, ex : "[bench] |  " ou "[redacteur] "
     * @param ps      resultat de OllamaPsInfo.query(), null si indisponible
     * @param gpuStats liste capturee par NvidiaSmiSnapshot.capture()
     */
    public static void print(String prefix, OllamaPsInfo ps, List<NvidiaSmiSnapshot.GpuStat> gpuStats) {
        if (ps != null) {
            System.out.printf("%sollama ps  -> %-10s  |  %s%n",
                    prefix, ps.sizeLabel(), ps.processorLabel());
        }
        if (gpuStats != null && !gpuStats.isEmpty()) {
            System.out.printf("%snvidia-smi -> %s%n",
                    prefix, NvidiaSmiSnapshot.formatShort(gpuStats));
        }
    }
}
