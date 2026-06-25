package storymagine.commun.infra;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Snapshot nvidia-smi : état mémoire et utilisation de chaque GPU.
 */
public class NvidiaSmiSnapshot {

    public record GpuStat(int index, String name, int usedMb, int freeMb, int totalMb, int utilPct) {}

    public static List<GpuStat> capture() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "nvidia-smi",
                "--query-gpu=index,name,memory.used,memory.free,memory.total,utilization.gpu",
                "--format=csv,noheader,nounits"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            List<GpuStat> result = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] parts = line.split(", ");
                    int n = parts.length;
                    if (n < 6) continue;
                    try {
                        int idx   = Integer.parseInt(parts[0].trim());
                        int used  = Integer.parseInt(parts[n - 4].trim());
                        int free  = Integer.parseInt(parts[n - 3].trim());
                        int total = Integer.parseInt(parts[n - 2].trim());
                        int util  = Integer.parseInt(parts[n - 1].trim());
                        String name = String.join(", ", Arrays.copyOfRange(parts, 1, n - 4)).trim();
                        result.add(new GpuStat(idx, name, used, free, total, util));
                    } catch (NumberFormatException ignored) {}
                }
            }
            proc.waitFor();
            return result;
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    /** "GPU 0 RTX5070 : 4209/8151 Mo  |  GPU 1 RTX5060Ti : 3566/16311 Mo" */
    public static String formatShort(List<GpuStat> stats) {
        if (stats.isEmpty()) return "(nvidia-smi indisponible)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stats.size(); i++) {
            if (i > 0) sb.append("  |  ");
            GpuStat g = stats.get(i);
            sb.append(String.format("GPU %d %s : %d/%d Mo", g.index(), g.name(), g.usedMb(), g.totalMb()));
        }
        return sb.toString();
    }

    public static String menuLine(GpuStat g) {
        return String.format("GPU %d  %-32s  %.0f Go", g.index(), g.name(), g.totalMb() / 1024.0);
    }
}
