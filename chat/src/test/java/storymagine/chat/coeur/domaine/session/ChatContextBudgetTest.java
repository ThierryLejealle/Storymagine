package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatContextBudgetTest {

    @Test
    void estimateTokensIsCharsDividedByFour() {
        assertEquals(0, ChatContextBudget.estimateTokens(""));
        assertEquals(0, ChatContextBudget.estimateTokens(null));
        assertEquals(25, ChatContextBudget.estimateTokens("x".repeat(100)));
    }

    @Test
    void turnsBudgetIsHalfTheWindowWhenFixedPartsAreSmall() {
        // fenetre 10000, parties fixes minimes (200 tokens) -> le plafond fixe (moitie) est le plus
        // restrictif des deux, donc c'est lui qui gagne.
        assertEquals(5000, ChatContextBudget.turnsBudget(10_000, 200));
    }

    @Test
    void turnsBudgetShrinksWhenFixedPartsGrowLarge() {
        // fenetre 10000, parties fixes tres grosses (8000 tokens, ex. gros scenario/personnage) ->
        // le plafond dynamique (fenetre - fixe - reserve) devient le plus restrictif.
        int budget = ChatContextBudget.turnsBudget(10_000, 8_000);
        assertEquals(10_000 - 8_000 - 1_024, budget);
    }

    @Test
    void turnsBudgetNeverGoesNegative() {
        // parties fixes a elles seules depassent deja la fenetre -> plancher a 0, pas de negatif.
        assertEquals(0, ChatContextBudget.turnsBudget(1_000, 5_000));
    }
}
