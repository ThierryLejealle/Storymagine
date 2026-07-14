package storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor;

import org.junit.jupiter.api.Test;
import storymagine.redacteur.coeur.domaine.orchestrator.CapturingLogPort;
import storymagine.redacteur.coeur.domaine.orchestrator.MockModelCallPort;

import static org.junit.jupiter.api.Assertions.*;

/** Direct unit tests for PhraseExtractor.parseResponse (no LLM) and PhraseExtractor.call() wiring
 *  (stubbed LLM, no real Ollama call — see class rule "on ne lance jamais de test unitaire avec un
 *  vrai agent LLM"). Written after a real production failure (2026-07-13) where a degenerate
 *  32k-token, no-answer response was silently treated as an ordinary NOT_FOUND. */
class PhraseExtractorTest {

    @Test
    void thinksIsFalse_mechanicalTextSearchDoesNotNeedReasoning() {
        var extractor = new PhraseExtractor(MockModelCallPort.uniform(8192, "NOT_FOUND"), new CapturingLogPort());
        assertFalse(extractor.thinks());
    }

    @Test
    void degenerateResponseWithoutPassageOrNotFound_logsAWarningAndReturnsNotFound() {
        CapturingLogPort log = new CapturingLogPort();
        // Simule une reponse qui a degenere sans jamais produire la ligne PASSAGE:/NOT_FOUND attendue.
        var llm = MockModelCallPort.uniform(8192, "blah blah blah ".repeat(50));
        var extractor = new PhraseExtractor(llm, log);

        PhraseExtractorOutput out = extractor.call(new PhraseExtractorInput("Le texte source.", "citation manquee"));

        assertFalse(out.found());
        assertEquals(1, log.warnings.size(), "une reponse inexploitable doit etre signalee, pas confondue avec un vrai NOT_FOUND");
        assertTrue(log.warnings.get(0).contains("SequencePhraseExtractor"));
    }

    @Test
    void genuineNotFound_doesNotLogAWarning() {
        CapturingLogPort log = new CapturingLogPort();
        var llm = MockModelCallPort.uniform(8192, "NOT_FOUND");
        var extractor = new PhraseExtractor(llm, log);

        PhraseExtractorOutput out = extractor.call(new PhraseExtractorInput("Le texte source.", "citation manquee"));

        assertFalse(out.found());
        assertTrue(log.warnings.isEmpty(), "un vrai NOT_FOUND ne doit pas etre signale comme une anomalie");
    }

    @Test
    void passageFound_doesNotLogAWarning() {
        CapturingLogPort log = new CapturingLogPort();
        var llm = MockModelCallPort.uniform(8192, "PASSAGE: \"Elle se repositionna pour le tir.\"");
        var extractor = new PhraseExtractor(llm, log);

        PhraseExtractorOutput out = extractor.call(new PhraseExtractorInput("Le texte source.", "citation manquee"));

        assertTrue(out.found());
        assertTrue(log.warnings.isEmpty());
    }

    @Test
    void passageFound_returnsCleanPhrase() {
        String response = "PASSAGE: \"Elle se repositionna pour le tir.\"";
        PhraseExtractorOutput out = PhraseExtractor.parseResponse(response);
        assertTrue(out.found());
        assertEquals("Elle se repositionna pour le tir.", out.phrase());
    }

    @Test
    void passageFound_stripsMixedQuoteCharacters() {
        String response = "PASSAGE: “Elle se repositionna pour le tir.”";
        PhraseExtractorOutput out = PhraseExtractor.parseResponse(response);
        assertTrue(out.found());
        assertEquals("Elle se repositionna pour le tir.", out.phrase());
    }

    @Test
    void notFoundSentinel_returnsNotFound() {
        PhraseExtractorOutput out = PhraseExtractor.parseResponse("NOT_FOUND");
        assertFalse(out.found());
        assertNull(out.phrase());
    }

    @Test
    void nullResponse_returnsNotFound() {
        PhraseExtractorOutput out = PhraseExtractor.parseResponse(null);
        assertFalse(out.found());
    }

    @Test
    void blankPassage_returnsNotFound() {
        PhraseExtractorOutput out = PhraseExtractor.parseResponse("PASSAGE: \"\"");
        assertFalse(out.found());
    }

    @Test
    void passageOnASecondLine_stillParsed() {
        String response = """
                Voici ma réponse :
                PASSAGE: "Elle se repositionna pour le tir."
                """;
        PhraseExtractorOutput out = PhraseExtractor.parseResponse(response);
        assertTrue(out.found());
        assertEquals("Elle se repositionna pour le tir.", out.phrase());
    }
}
