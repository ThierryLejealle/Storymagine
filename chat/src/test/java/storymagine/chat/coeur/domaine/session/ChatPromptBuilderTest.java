package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPromptBuilderTest {

    private final ChatScenario scenario = new ChatScenario("test", "A grumpy innkeeper.", "A stormy night at the inn.", List.of(), "");

    private final ChatScenario scenarioWithActs = new ChatScenario("test", "A grumpy innkeeper.",
        "A stormy night at the inn.", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Act one: the storm begins."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two: a stranger arrives."),
            ScenarioAct.leaf(ActNumber.of(3), "", "Act three: the truth comes out.")), "");

    @Test
    void systemCarriesCharacterAndScenario() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", List.of(), PlayerMessage.parse("Hi."));
        assertTrue(prompt.system().contains("A grumpy innkeeper."));
        assertTrue(prompt.system().contains("A stormy night at the inn."));
    }

    @Test
    void userIsThePlayerLinePrefixedAndEndsWithThePrefillWhenNoHistoryYet() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", List.of(), PlayerMessage.parse("I need a room."));
        assertEquals("Player: I need a room.\n\nCharacter:", prompt.user());
    }

    @Test
    void currentPlayerLineAppearsExactlyOnceAcrossSystemAndUser() {
        List<ChatTurn> history = List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hello."),
            new ChatTurn(ChatTurn.Speaker.LLM, "*grunts* What do you want?"));

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", history, PlayerMessage.parse("I need a room."));
        String combined = prompt.system() + "\n" + prompt.user();

        int occurrences = combined.split("I need a room\\.", -1).length - 1;
        assertEquals(1, occurrences, "le message du joueur ne doit apparaitre qu'une seule fois dans le prompt");
    }

    @Test
    void narratorHandoffIsTaggedDoInTheUserMessage() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", List.of(), PlayerMessage.parse("DO: the storm knocks out the lights."));
        assertEquals("Player: DO: the storm knocks out the lights.\n\nCharacter:", prompt.user());
    }

    @Test
    void blankSummaryIsOmittedFromSystem() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", List.of(), PlayerMessage.parse("Hi."));
        assertFalse(prompt.system().contains("STORY SO FAR"));
    }

    @Test
    void nonBlankSummaryIsIncludedInSystem() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "They met at the market.", List.of(), PlayerMessage.parse("Hi."));
        assertTrue(prompt.system().contains("STORY SO FAR:\nThey met at the market."));
    }

    @Test
    void recentHistoryIsFoldedIntoTheUserMessageAndEndsWithThePrefill() {
        List<ChatTurn> history = List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back."));

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", history, PlayerMessage.parse("How are you?"));

        assertTrue(prompt.user().contains("Player: Hi."));
        assertTrue(prompt.user().contains("Character: Hello back."));
        assertTrue(prompt.user().contains("Player: How are you?"));
        assertTrue(prompt.user().trim().endsWith("Character:"));
    }

    @Test
    void transcriptFormatsPlayerAndLlmTurnsWithTheGivenCharacterLabel() {
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back.")), "Character");
        assertEquals("Player: Hi.\nCharacter: Hello back.\n", t);
    }

    @Test
    void transcriptUsesTheCharacterNameInsteadOfTheGenericLabelWhenGiven() {
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back.")), "Sylka");
        assertEquals("Sylka: Hello back.\n", t);
    }

    @Test
    void transcriptFormatsNarratorTurns() {
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), "Character");
        assertEquals("Narration: The door creaks open.\nPlayer: Hi.\n", t);
    }

    @Test
    void characterLabelFallsBackToGenericWhenScenarioHasNoName() {
        assertEquals("Character", ChatPromptBuilder.characterLabel(scenario));
    }

    @Test
    void characterLabelUsesTheScenariosCharacterNameWhenPresent() {
        ChatScenario named = new ChatScenario("test", "sheet", "premise", List.of(), "Sylka");
        assertEquals("Sylka", ChatPromptBuilder.characterLabel(named));
    }

    @Test
    void userPrefillUsesTheCharacterNameWhenPresent() {
        ChatScenario named = new ChatScenario("test", "sheet", "premise", List.of(), "Sylka");
        ChatPrompt prompt = ChatPromptBuilder.build(named, 0, "", List.of(), PlayerMessage.parse("Hi."));
        assertTrue(prompt.user().endsWith("Sylka:"));
    }

    // ── Progressive acts ────────────────────────────────────────────────────

    @Test
    void currentActZeroOmitsCurrentActSection() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 0, "", List.of(), PlayerMessage.parse("Hi."));
        assertFalse(prompt.system().contains("CURRENT ACT"));
    }

    @Test
    void actTitleIsNeverSentToTheLlm() {
        ChatScenario scenarioWithTitledAct = new ChatScenario("test", "A grumpy innkeeper.",
            "A stormy night at the inn.", List.of(ScenarioAct.leaf(ActNumber.of(1), "Should never reach the LLM", "Act body.")), "");

        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithTitledAct, 1, "", List.of(), PlayerMessage.parse("Hi."));

        assertFalse(prompt.system().contains("Should never reach the LLM"));
        assertTrue(prompt.system().contains("Act body."));
    }

    @Test
    void currentActIncludesOnlyThatActsTextNotTheOthers() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithActs, 2, "", List.of(), PlayerMessage.parse("Hi."));
        assertTrue(prompt.system().contains("CURRENT ACT (2 of 3)"));
        assertTrue(prompt.system().contains("Act two: a stranger arrives."));
        assertFalse(prompt.system().contains("Act one: the storm begins."));
        assertFalse(prompt.system().contains("Act three: the truth comes out."));
    }

    @Test
    void nextActRuleIsOfferedWhenMoreActsRemain() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithActs, 1, "", List.of(), PlayerMessage.parse("Hi."));
        assertTrue(prompt.system().contains("[NEXT ACT]"));
    }

    @Test
    void nextActRuleIsOmittedOnTheLastAct() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithActs, 3, "", List.of(), PlayerMessage.parse("Hi."));
        assertFalse(prompt.system().contains("[NEXT ACT]"));
    }

    @Test
    void defaultNextActRuleIsUsedWhenActTextHasNoBracketedMarker() {
        // "next act" apparait en toutes lettres dans une phrase anodine, sans les crochets du
        // marqueur reel — ne doit PAS declencher la regle conditionnelle (detection par sous-chaine
        // libre autrefois trop permissive, voir revue Fable).
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "In the next act of her plan, she reveals nothing."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")), "");

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 1, "", List.of(), PlayerMessage.parse("Hi."));

        assertTrue(prompt.system().contains("you feel this act's events have reached their natural conclusion"));
        assertFalse(prompt.system().contains("tells you exactly when to write"));
    }

    @Test
    void conditionalNextActRuleIsUsedWhenActTextHasTheBracketedMarker() {
        ChatScenario scenario = new ChatScenario("test", "sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Quand écrire [NEXT ACT] : quand la porte s'ouvre."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")), "");

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, 1, "", List.of(), PlayerMessage.parse("Hi."));

        assertTrue(prompt.system().contains("tells you exactly when to write"));
        assertFalse(prompt.system().contains("you feel this act's events have reached their natural conclusion"));
    }
}
