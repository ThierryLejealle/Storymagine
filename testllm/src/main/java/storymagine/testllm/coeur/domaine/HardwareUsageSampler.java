package storymagine.testllm.coeur.domaine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Echantillonne l'utilisation GPU et CPU toutes les 10 secondes pendant un benchmark de modèle.
 */
public class HardwareUsageSampler {

    private static final int POLL_MS = 10_000;

    private final Supplier<HardwareUsage> supplier;
    private final List<HardwareUsage>     samples = new ArrayList<>();
    private volatile boolean              running = false;
    private Thread                        thread;

    public HardwareUsageSampler(Supplier<HardwareUsage> supplier) {
        this.supplier = supplier;
    }

    public void start() {
        running = true;
        thread  = Thread.ofVirtual().start(() -> {
            while (running) {
                HardwareUsage sample = supplier.get();
                if (sample.gpu0Pct() >= 0 || sample.gpu1Pct() >= 0 || sample.cpuPct() >= 0) {
                    samples.add(sample);
                }
                try { Thread.sleep(POLL_MS); } catch (InterruptedException e) { break; }
            }
        });
    }

    public void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try { thread.join(3000); } catch (InterruptedException ignored) {}
        }
    }

    /** Retourne la moyenne des échantillons collectés, ou {@link HardwareUsage#ABSENT} si aucun. */
    public HardwareUsage avg() {
        if (samples.isEmpty()) return HardwareUsage.ABSENT;
        int gpu0 = avgPct(samples.stream().mapToInt(HardwareUsage::gpu0Pct).filter(v -> v >= 0).toArray());
        int gpu1 = avgPct(samples.stream().mapToInt(HardwareUsage::gpu1Pct).filter(v -> v >= 0).toArray());
        int cpu  = avgPct(samples.stream().mapToInt(HardwareUsage::cpuPct).filter(v -> v >= 0).toArray());
        return new HardwareUsage(gpu0, gpu1, cpu);
    }

    private static int avgPct(int[] values) {
        if (values.length == 0) return -1;
        return (int) Math.round(Arrays.stream(values).average().orElse(-1));
    }
}
