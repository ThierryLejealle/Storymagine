package storymagine.testllm.infra;

import storymagine.commun.infra.OllamaConfig;
import storymagine.commun.infra.RetryPolicy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Lit testllm.properties et expose la configuration du module.
 */
public class TestLlmConfig {

    private final Properties props;

    private TestLlmConfig(Properties props) {
        this.props = props;
    }

    public static TestLlmConfig load(Path propsFile) {
        Properties p = new Properties();
        if (Files.exists(propsFile)) {
            try (InputStream in = Files.newInputStream(propsFile)) {
                p.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Impossible de lire " + propsFile + " : " + e.getMessage(), e);
            }
        }
        return new TestLlmConfig(p);
    }

    public List<String> favoris() {
        List<String> result = new ArrayList<>();
        for (int i = 1; ; i++) {
            String v = props.getProperty("favoris." + i);
            if (v == null || v.isBlank()) break;
            result.add(v.trim());
        }
        return result;
    }

    public int     runs()                      { return intProp("bench.runs",                    3); }
    public long    maxModelSizeBytes()         { return longProp("bench.maxModelSizeGo",         14) * 1_073_741_824L; }
    public String  outputDir()                 { return props.getProperty("bench.outputDir",     "bench"); }

    public OllamaConfig ollamaConfig() {
        RetryPolicy retry = new RetryPolicy(
            intProp("ollama.retryCount",            2),
            intProp("ollama.retryDelay1",           15),
            intProp("ollama.retryDelay2",           30),
            intProp("ollama.retryDelay3Plus",       60)
        );
        return new OllamaConfig(
            props.getProperty("ollama.url",         "http://localhost:11434"),
            intProp("ollama.contextWindow",          32768),
            intProp("ollama.maxContextWindow",       131072),
            intProp("ollama.topK",                   40),
            doubleProp("ollama.topP",                0.9),
            doubleProp("ollama.repeatPenalty",       1.1),
            intProp("ollama.numPredict",             -1),
            intProp("ollama.timeoutMs",              1200000),
            retry,
            intProp("ollama.largeModelRamFractionPct",   60),
            intProp("ollama.largeModelTimeoutMultiplier", 2)
        );
    }

    private int    intProp(String key, int def) {
        String v = props.getProperty(key);
        return (v != null && !v.isBlank()) ? Integer.parseInt(v.trim()) : def;
    }

    private long   longProp(String key, long def) {
        String v = props.getProperty(key);
        return (v != null && !v.isBlank()) ? Long.parseLong(v.trim()) : def;
    }

    private double doubleProp(String key, double def) {
        String v = props.getProperty(key);
        return (v != null && !v.isBlank()) ? Double.parseDouble(v.trim()) : def;
    }
}
