package storymagine.commun.coeur.domaine.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromptBuilderTest {

    @Test
    void skipsBlankSections() {
        String result = PromptBuilder.create()
                .section("Titre A", "")
                .section("Titre B", null)
                .section("Titre B", "   ")
                .build();

        assertEquals("", result);
    }

    @Test
    void joinsNonBlankSectionsWithBlankLine() {
        String result = PromptBuilder.create()
                .section("Texte", "Contenu du texte")
                .section("Points à vérifier", "Point 1\nPoint 2")
                .build();

        assertEquals("### Texte\nContenu du texte\n\n### Points à vérifier\nPoint 1\nPoint 2", result);
    }

    @Test
    void skippedSectionDoesNotLeaveAGap() {
        String result = PromptBuilder.create()
                .section("Premier", "A")
                .section("Vide", "")
                .section("Dernier", "B")
                .build();

        assertEquals("### Premier\nA\n\n### Dernier\nB", result);
    }

    @Test
    void sectionOrElseFallsBackWhenBlank() {
        String result = PromptBuilder.create()
                .sectionOrElse("Focus", "", "Aucun élément de focus.")
                .build();

        assertEquals("### Focus\nAucun élément de focus.", result);
    }

    @Test
    void sectionOrElseKeepsContentWhenPresent() {
        String result = PromptBuilder.create()
                .sectionOrElse("Focus", "Un élément précis", "Aucun élément de focus.")
                .build();

        assertEquals("### Focus\nUn élément précis", result);
    }

    @Test
    void rawAppendsFreeTextWithoutTitle() {
        String result = PromptBuilder.create()
                .section("Texte", "Contenu")
                .raw("Évalue la cohérence du texte.")
                .build();

        assertEquals("### Texte\nContenu\n\nÉvalue la cohérence du texte.", result);
    }

    @Test
    void blankRawIsSkipped() {
        String result = PromptBuilder.create()
                .raw("")
                .raw(null)
                .section("Texte", "Contenu")
                .build();

        assertEquals("### Texte\nContenu", result);
    }
}
