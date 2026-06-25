package storymagine.testllm.infra;

import storymagine.commun.infra.NvidiaSmiSnapshot;
import storymagine.testllm.coeur.domaine.PasseBench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Ecrit les fichiers de log detailles par modele/passe (un .md par fichier).
 */
public class BenchLogWriter {

    public void write(Path logDir, PasseBench passe, String model, boolean think,
                      List<Long> elapsed, List<Integer> toks, List<Double> tpsList,
                      List<String> texts, GpuState gpuState) {
        String safe     = model.replaceAll("[:/\\\\<>\"*|?]", "_");
        String filename = safe + (think ? "_think" : "") + ".md";
        Path   file     = logDir.resolve(passe.nom()).resolve(filename);

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(passe.nom()).append(" — ").append(model);
        if (think) sb.append(" [think:O]");
        sb.append("\n\n");

        if (gpuState != null && !GpuState.ABSENT.equals(gpuState)) {
            sb.append("## GPU STATE\n");
            sb.append("- Mode GPU   : ").append(gpuState.gpuMode()).append("\n");
            if (gpuState.ps() != null) {
                sb.append(String.format("- ollama ps  : %-10s  |  %s%n",
                        gpuState.ps().sizeLabel(), gpuState.ps().processorLabel()));
            }
            for (NvidiaSmiSnapshot.GpuStat g : gpuState.gpuStats()) {
                sb.append(String.format("- GPU %d      : %-30s  %5d / %5d Mo%n",
                        g.index(), g.name(), g.usedMb(), g.totalMb()));
            }
            sb.append("\n");
        }

        if (!passe.systemPrompt().isBlank())
            sb.append("## PROMPT SYSTEME\n\n").append(passe.systemPrompt()).append("\n\n");
        sb.append("## PROMPT UTILISATEUR\n\n").append(passe.userPrompt()).append("\n\n");

        for (int i = 0; i < texts.size(); i++) {
            sb.append("---\n\n## RUN ").append(i + 1).append("\n");
            if (i < elapsed.size()) {
                sb.append(String.format("- Duree    : %5.1fs%n", elapsed.get(i) / 1000.0));
                if (i < toks.size())    sb.append(String.format("- Resultat : %d car%n", toks.get(i)));
                if (i < tpsList.size()) sb.append(String.format("- t/s      : %.1f%n", tpsList.get(i)));
            }
            sb.append("\n").append(texts.get(i)).append("\n\n");
        }

        if (elapsed.size() > 1) {
            double avgMs  = elapsed.stream().mapToLong(Long::longValue).average().orElse(0);
            int    minTok = toks.stream().mapToInt(Integer::intValue).min().orElse(0);
            int    maxTok = toks.stream().mapToInt(Integer::intValue).max().orElse(0);
            int    avgTok = (int) toks.stream().mapToInt(Integer::intValue).average().orElse(0);
            double avgTps = tpsList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            sb.append("---\n\n## RESUME\n");
            sb.append(String.format("- car min/moy/max : %d / %d / %d%n", minTok, avgTok, maxTok));
            sb.append(String.format("- temps moy (s)   : %.1f%n", avgMs / 1000.0));
            sb.append(String.format("- t/s moy         : %.1f%n", avgTps));
        }

        try {
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.printf("[bench] Erreur ecriture log %s : %s%n", file, e.getMessage());
        }
    }
}
