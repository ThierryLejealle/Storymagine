package storymagine.chat.infra;

import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;
import storymagine.chat.coeur.ports.ScenarioTestExportPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScenarioTestFileExportAdapter implements ScenarioTestExportPort {

    @Override
    public Path export(ScenarioTestReport report, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Path file = outputDir.resolve("report.html");
        Files.writeString(file, ScenarioTestHtmlExporter.generate(report), StandardCharsets.UTF_8);
        return file;
    }
}
