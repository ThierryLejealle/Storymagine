package storymagine.testllm.coeur.domaine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Echantillonne la consommation mémoire d'un modèle pendant un benchmark.
 * Le fournisseur de valeur (VRAM, RAM…) est injecté à la construction.
 */
public class MemorySampler {

    private final LongSupplier   vramSupplier;
    private final List<Long>     samples = new ArrayList<>();
    private volatile boolean     running = false;
    private Thread               thread;

    public MemorySampler(LongSupplier vramSupplier) {
        this.vramSupplier = vramSupplier;
    }

    public void start() {
        running = true;
        thread  = Thread.ofVirtual().start(() -> {
            while (running) {
                long mb = vramSupplier.getAsLong();
                if (mb > 0) samples.add(mb);
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
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

    /** Moyenne en Mo, ou -1 si aucun échantillon. */
    public long avgMb() {
        if (samples.isEmpty()) return -1;
        return (long) samples.stream().mapToLong(Long::longValue).average().orElse(-1);
    }
}
