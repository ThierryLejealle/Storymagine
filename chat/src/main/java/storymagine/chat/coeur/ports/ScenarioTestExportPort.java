package storymagine.chat.coeur.ports;

import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;

import java.io.IOException;
import java.nio.file.Path;

/** Writes a ScenarioTestReport to disk as a human-readable report, returning the written file's path. */
public interface ScenarioTestExportPort {

    Path export(ScenarioTestReport report, Path outputDir) throws IOException;
}
