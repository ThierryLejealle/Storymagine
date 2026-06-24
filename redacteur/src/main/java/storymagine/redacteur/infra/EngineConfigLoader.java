package storymagine.redacteur.infra;

import storymagine.redacteur.coeur.domaine.orchestrator.EngineConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Loads {@link EngineConfig} from an engine.properties file. Falls back to defaults if absent. */
public final class EngineConfigLoader {

    private EngineConfigLoader() {}

    public static EngineConfig load(Path file) {
        Properties props = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.printf("[engine] %s illisible : %s — valeurs par defaut utilisees%n",
                        file.getFileName(), e.getMessage());
            }
        }
        return new EngineConfig(
                parseInt(props.getProperty("critique.plan.max_retry"),     3),
                parseInt(props.getProperty("critique.chapitre.max_retry"), 3),
                parseDouble(props.getProperty("critique.chapitre.threshold"), 7.0),
                parseInt(props.getProperty("critique.sequence.max_retry"), 1)
        );
    }

    private static int parseInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static double parseDouble(String s, double def) {
        if (s == null || s.isBlank()) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
