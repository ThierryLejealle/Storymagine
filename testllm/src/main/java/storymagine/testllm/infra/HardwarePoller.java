package storymagine.testllm.infra;

import storymagine.testllm.coeur.domaine.HardwareUsage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fournit un Supplier<HardwareUsage> combinant nvidia-smi (GPU util%) et typeperf (CPU%).
 */
public class HardwarePoller {

    public static Supplier<HardwareUsage> create() {
        return () -> {
            List<NvidiaSmiSnapshot.GpuStat> gpus = NvidiaSmiSnapshot.capture();
            int gpu0 = gpus.stream().filter(g -> g.index() == 0)
                           .mapToInt(NvidiaSmiSnapshot.GpuStat::utilPct).findFirst().orElse(-1);
            int gpu1 = gpus.stream().filter(g -> g.index() == 1)
                           .mapToInt(NvidiaSmiSnapshot.GpuStat::utilPct).findFirst().orElse(-1);
            int cpu  = readCpuPct();
            return new HardwareUsage(gpu0, gpu1, cpu);
        };
    }

    // typeperf counter paths vary by Windows locale — used as fallback only
    private static final String[] CPU_COUNTERS_FALLBACK = {
        "\\Processeur(_Total)\\% temps processeur",  // French Windows
        "\\Processor(_Total)\\% Processor Time"       // English Windows
    };

    private static int readCpuPct() {
        // Primary: Win32_Processor.LoadPercentage via CIM — locale-independent
        int cim = readCpuViaCim();
        if (cim >= 0) return cim;
        // Fallback: typeperf with localized counter paths
        for (String counter : CPU_COUNTERS_FALLBACK) {
            int val = tryTypeperf(counter);
            if (val >= 0) return val;
        }
        return -1;
    }

    private static int readCpuViaCim() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "(Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) output = line;
                }
            }
            proc.waitFor();
            if (output != null) return (int) Math.round(Double.parseDouble(output.replace(",", ".")));
        } catch (Exception ignored) {}
        return -1;
    }

    private static int tryTypeperf(String counter) {
        try {
            ProcessBuilder pb = new ProcessBuilder("typeperf", counter, "-sc", "1");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String lastValue = null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("(")) continue;
                    // CSV: "timestamp","value" — split on "," to handle locale decimal separators
                    String[] parts = line.split("\",\"");
                    if (parts.length >= 2) {
                        String raw = parts[parts.length - 1].replace("\"", "").replace(",", ".").trim();
                        try { Double.parseDouble(raw); lastValue = raw; } catch (NumberFormatException ignored) {}
                    }
                }
            }
            proc.waitFor();
            if (lastValue != null) return (int) Math.round(Double.parseDouble(lastValue));
        } catch (Exception ignored) {}
        return -1;
    }
}
