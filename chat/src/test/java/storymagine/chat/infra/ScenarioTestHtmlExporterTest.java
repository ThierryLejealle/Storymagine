package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenariotester.ActTestResult;
import storymagine.chat.coeur.domaine.scenariotester.ScenarioTestReport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioTestHtmlExporterTest {

    @Test
    void includesActTitlesIssuesAndSuggestions() {
        ActTestResult act = new ActTestResult(ActNumber.of(1, 2), "The storm breaks",
            List.of("Contradicts act 1"), List.of("NEXT ACT condition is vague"),
            List.of("[Continuité] Introduce the storm earlier"));
        ScenarioTestReport report = new ScenarioTestReport("temple-noir-actes", List.of(act));

        String html = ScenarioTestHtmlExporter.generate(report);

        assertTrue(html.contains("temple-noir-actes"));
        assertTrue(html.contains("1.2"));
        assertTrue(html.contains("The storm breaks"));
        assertTrue(html.contains("Contradicts act 1"));
        assertTrue(html.contains("NEXT ACT condition is vague"));
        assertTrue(html.contains("Introduce the storm earlier"));
    }

    @Test
    void showsANoneMessageWhenAnActHasNothingToReport() {
        ActTestResult act = new ActTestResult(ActNumber.of(1), "Arrival", List.of(), List.of(), List.of());
        ScenarioTestReport report = new ScenarioTestReport("inn-test", List.of(act));

        String html = ScenarioTestHtmlExporter.generate(report);

        assertTrue(html.contains("Rien à signaler."));
    }

    @Test
    void escapesHtmlSpecialCharactersInLlmText() {
        ActTestResult act = new ActTestResult(ActNumber.of(1), "Arrival",
            List.of("<script>alert(1)</script>"), List.of(), List.of());
        ScenarioTestReport report = new ScenarioTestReport("inn-test", List.of(act));

        String html = ScenarioTestHtmlExporter.generate(report);

        assertFalse(html.contains("<script>alert(1)</script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
}
