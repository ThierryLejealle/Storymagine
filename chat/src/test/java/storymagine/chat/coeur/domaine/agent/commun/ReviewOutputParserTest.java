package storymagine.chat.coeur.domaine.agent.commun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewOutputParserTest {

    @Test
    void parsesBothSectionsWithItems() {
        String raw = """
            ISSUES:
            - first issue
            - second issue
            SUGGESTIONS:
            - first suggestion""";

        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);

        assertEquals(java.util.List.of("first issue", "second issue"), review.issues());
        assertEquals(java.util.List.of("first suggestion"), review.suggestions());
    }

    @Test
    void treatsRienSentinelAsAnEmptySection() {
        String raw = """
            ISSUES:
            [RIEN]
            SUGGESTIONS:
            [RIEN]""";

        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);

        assertTrue(review.issues().isEmpty());
        assertTrue(review.suggestions().isEmpty());
    }

    @Test
    void acceptsLinesWithoutTheDashPrefix() {
        String raw = "ISSUES:\nsomething is off\nSUGGESTIONS:\n[RIEN]";

        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);

        assertEquals(java.util.List.of("something is off"), review.issues());
    }

    @Test
    void headersAreCaseInsensitive() {
        String raw = "issues:\n- a problem\nsuggestions:\n[rien]";

        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);

        assertEquals(java.util.List.of("a problem"), review.issues());
        assertTrue(review.suggestions().isEmpty());
    }

    @Test
    void ignoresTextBeforeTheFirstHeader() {
        String raw = "Sure, here is my analysis:\nISSUES:\n- a problem\nSUGGESTIONS:\n[RIEN]";

        ReviewOutputParser.Review review = ReviewOutputParser.parse(raw);

        assertEquals(java.util.List.of("a problem"), review.issues());
    }

    @Test
    void returnsEmptyReviewForNullOrBlankInput() {
        assertTrue(ReviewOutputParser.parse(null).issues().isEmpty());
        assertTrue(ReviewOutputParser.parse(null).suggestions().isEmpty());
        assertTrue(ReviewOutputParser.parse("   ").issues().isEmpty());
    }
}
