package storymagine.chat.coeur.domaine.session;

import org.junit.jupiter.api.Test;
import storymagine.chat.coeur.domaine.scenario.ActNumber;
import storymagine.chat.coeur.domaine.scenario.Cast;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.scenario.Npc;
import storymagine.chat.coeur.domaine.scenario.ScenarioAct;
import storymagine.chat.coeur.domaine.session.ChatPromptBuilder.ChatPrompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPromptBuilderTest {

    private static ChatScenario scenarioWith(String characterSheet, String premise, List<ScenarioAct> acts) {
        return new ChatScenario("test", new Cast(List.of(new Npc("character", "", characterSheet, ""))), premise, acts);
    }

    private static Scene sceneFor(ChatScenario scenario) {
        return new Scene(scenario.cast().npcs().get(0), List.of(), List.of(), false);
    }

    private final ChatScenario scenario = scenarioWith("A grumpy innkeeper.", "A stormy night at the inn.", List.of());

    private final ChatScenario scenarioWithActs = scenarioWith("A grumpy innkeeper.", "A stormy night at the inn.", List.of(
        ScenarioAct.leaf(ActNumber.of(1), "", "Act one: the storm begins."),
        ScenarioAct.leaf(ActNumber.of(2), "", "Act two: a stranger arrives."),
        ScenarioAct.leaf(ActNumber.of(3), "", "Act three: the truth comes out.")));

    @Test
    void systemCarriesCharacterAndScenario() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", List.of());
        assertTrue(prompt.system().contains("A grumpy innkeeper."));
        assertTrue(prompt.system().contains("A stormy night at the inn."));
    }

    @Test
    void userIsThePlayerLinePrefixedAndEndsWithThePrefillWhenNoHistoryYet() {
        List<ChatTurn> recent = List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "I need a room."));
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", recent);
        // Le PNJ de test n'a pas de nom declare (voir scenarioWith) : Npc.label() retombe sur son
        // id ("character"), pas sur un "Character" generique fige — voir Npc.label(). Le scenario
        // de test ne declare pas non plus de "Joueur : ..." : ChatScenario.playerName retombe sur
        // "Alex" (voir ChatScenario).
        assertEquals("Recent exchange:\nAlex: I need a room.\n\ncharacter:", prompt.user());
    }

    @Test
    void currentPlayerLineAppearsExactlyOnceInTheUserMessage() {
        // recentTurns se termine toujours par la ligne courante du joueur (voir build) — aucun
        // trailer separe n'existe plus, donc pas de risque de duplication a tester au niveau du
        // builder lui-meme ; ce test verifie juste qu'elle apparait bien une fois dans le rendu.
        List<ChatTurn> history = List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hello."),
            new ChatTurn(ChatTurn.Speaker.LLM, "*grunts* What do you want?", "", "character"),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "I need a room."));

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", history);

        int occurrences = prompt.user().split("I need a room\\.", -1).length - 1;
        assertEquals(1, occurrences, "le message du joueur ne doit apparaitre qu'une seule fois dans le prompt");
    }

    @Test
    void doPrefixedLineIsEchoedVerbatimInTheTranscript() {
        List<ChatTurn> recent = List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "DO: the storm knocks out the lights."));
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", recent);
        assertEquals("Recent exchange:\nAlex: DO: the storm knocks out the lights.\n\ncharacter:", prompt.user());
    }

    @Test
    void blankSummaryIsOmittedFromSystem() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", List.of());
        assertFalse(prompt.system().contains("STORY SO FAR"));
    }

    @Test
    void nonBlankSummaryIsIncludedInSystem() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "They met at the market.", List.of());
        assertTrue(prompt.system().contains("STORY SO FAR:\nThey met at the market."));
    }

    @Test
    void recentHistoryIsFoldedIntoTheUserMessageAndEndsWithThePrefill() {
        List<ChatTurn> history = List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back.", "", "character"),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "How are you?"));

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", history);

        assertTrue(prompt.user().contains("Alex: Hi."));
        assertTrue(prompt.user().contains("character: Hello back."));
        assertTrue(prompt.user().contains("Alex: How are you?"));
        assertTrue(prompt.user().trim().endsWith("character:"));
    }

    @Test
    void transcriptFormatsPlayerAndLlmTurnsWithTheSpeakingNpcsName() {
        Cast cast = new Cast(List.of(new Npc("character", "Character", "", "")));
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back.", "", "character")), cast, "Player");
        assertEquals("Player: Hi.\nCharacter: Hello back.\n", t);
    }

    @Test
    void transcriptUsesTheNpcsOwnNameResolvedFromItsId() {
        Cast cast = new Cast(List.of(new Npc("sylka", "Sylka", "", "")));
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back.", "", "sylka")), cast, "Player");
        assertEquals("Sylka: Hello back.\n", t);
    }

    @Test
    void transcriptFallsBackToGenericCharacterLabelWhenATurnHasNoNpcId() {
        // Historique persiste avant l'introduction du Cast multi-PNJ (voir ChatTurn.npcId) — doit
        // continuer a se lire comme avant, sans lever d'erreur ni afficher "null".
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello back.")), new Cast(List.of()), "Player");
        assertEquals("Character: Hello back.\n", t);
    }

    @Test
    void transcriptFormatsNarratorTurns() {
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), new Cast(List.of()), "Player");
        assertEquals("Narration: The door creaks open.\nPlayer: Hi.\n", t);
    }

    @Test
    void transcriptUsesTheGivenPlayerNameInsteadOfTheGenericLabel() {
        String t = ChatPromptBuilder.transcript(List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "I open the door.")), new Cast(List.of()), "Alex");
        assertEquals("Alex: I open the door.\n", t);
    }

    @Test
    void userPrefillUsesTheSpeakingNpcsName() {
        Cast cast = new Cast(List.of(new Npc("sylka", "Sylka", "sheet", "")));
        ChatScenario named = new ChatScenario("test", cast, "premise", List.of());
        Scene scene = new Scene(cast.npcs().get(0), List.of(), List.of(), false);

        ChatPrompt prompt = ChatPromptBuilder.build(named, scene, 0, "", List.of());

        assertTrue(prompt.user().endsWith("Sylka:"));
    }

    // ── Multi-NPC : la scene ─────────────────────────────────────────────────

    @Test
    void speakerGetsTheirFullSheetIncludingSecretInfo() {
        Npc speaker = new Npc("elena", "Elena", "A sharp-tongued merchant.", "Secretly a smuggler.");
        ChatScenario named = new ChatScenario("test", new Cast(List.of(speaker)), "premise", List.of());

        ChatPrompt prompt = ChatPromptBuilder.build(named, new Scene(speaker, List.of(), List.of(), false), 0, "", List.of());

        assertTrue(prompt.system().contains("A sharp-tongued merchant."));
        assertTrue(prompt.system().contains("Secretly a smuggler."));
    }

    @Test
    void otherPresentNpcsShareTheirPublicSheetButNeverTheirSecret() {
        Npc speaker = new Npc("elena", "Elena", "Elena's sheet.", "");
        Npc other   = new Npc("marcus", "Marcus", "Marcus's public sheet text.", "Marcus's secret sheet text.");
        ChatScenario named = new ChatScenario("test", new Cast(List.of(speaker, other)), "premise", List.of());

        ChatPrompt prompt = ChatPromptBuilder.build(named, new Scene(speaker, List.of(other), List.of(), false), 0, "", List.of());

        assertTrue(prompt.system().contains("ALSO PRESENT"));
        assertTrue(prompt.system().contains("Marcus"));
        assertTrue(prompt.system().contains("Marcus's public sheet text."),
            "les coequipiers deja etablis connaissent la fiche publique des uns et des autres");
        assertFalse(prompt.system().contains("Marcus's secret sheet text."),
            "le SECRET d'un PNJ ne doit jamais fuiter dans le prompt d'un autre");
    }

    @Test
    void otherNpcsRuleIsOnlyAddedWhenSomeoneElseIsPresent() {
        Npc speaker = new Npc("elena", "Elena", "sheet", "");
        Npc other   = new Npc("marcus", "Marcus", "sheet", "");
        ChatScenario named = new ChatScenario("test", new Cast(List.of(speaker, other)), "premise", List.of());

        ChatPrompt alone = ChatPromptBuilder.build(named, new Scene(speaker, List.of(), List.of(), false), 0, "", List.of());
        ChatPrompt withOther = ChatPromptBuilder.build(named, new Scene(speaker, List.of(other), List.of(), false), 0, "", List.of());

        assertFalse(alone.system().contains("ALSO PRESENT"));
        // normalise les espaces/retours a la ligne : ce test ne doit pas dependre du retour a la
        // ligne exact choisi dans OTHER_NPCS_RULE (piege deja rencontre deux fois).
        String normalized = withOther.system().replaceAll("\\s+", " ");
        assertTrue(normalized.contains("never write dialogue or actions for another present character"));
    }

    @Test
    void absentTeammatesShareTheirPublicSheetButNeverTheirSecretAndAreNeverMadeToSpeak() {
        Npc speaker = new Npc("elena", "Elena", "Elena's sheet.", "");
        Npc absent  = new Npc("marcus", "Marcus", "Marcus's public sheet text.", "Marcus's secret sheet text.");
        ChatScenario named = new ChatScenario("test", new Cast(List.of(speaker, absent)), "premise", List.of());

        ChatPrompt prompt = ChatPromptBuilder.build(named, new Scene(speaker, List.of(), List.of(absent), false), 0, "", List.of());

        assertTrue(prompt.system().contains("NOT IN THE SCENE"));
        assertTrue(prompt.system().contains("Marcus's public sheet text."),
            "un coequipier absent reste connu, juste pas physiquement present");
        assertFalse(prompt.system().contains("Marcus's secret sheet text."),
            "le SECRET d'un PNJ absent ne doit pas plus fuiter que celui d'un PNJ present");
        assertTrue(prompt.system().replaceAll("\\s+", " ").contains("never have them speak, act, or walk in"));
    }

    @Test
    void notInTheSceneBlockIsOmittedWhenNobodyIsAbsent() {
        Npc speaker = new Npc("elena", "Elena", "sheet", "");
        ChatScenario named = new ChatScenario("test", new Cast(List.of(speaker)), "premise", List.of());

        ChatPrompt prompt = ChatPromptBuilder.build(named, new Scene(speaker, List.of(), List.of(), false), 0, "", List.of());

        assertFalse(prompt.system().contains("NOT IN THE SCENE"));
    }

    // ── Interjections ────────────────────────────────────────────────────────

    @Test
    void interjectionRuleIsOnlyAddedWhenSceneIsInterjecting() {
        Npc speaker = new Npc("marcus", "Marcus", "sheet", "");
        ChatScenario named = new ChatScenario("test", new Cast(List.of(speaker)), "premise", List.of());

        ChatPrompt normal = ChatPromptBuilder.build(named, new Scene(speaker, List.of(), List.of(), false), 0, "", List.of());
        ChatPrompt interjecting = ChatPromptBuilder.build(named, new Scene(speaker, List.of(), List.of(), true), 0, "", List.of());

        assertFalse(normal.system().contains("This is an interjection"));
        assertTrue(interjecting.system().contains("This is an interjection"));
    }

    // ── Progressive acts ────────────────────────────────────────────────────

    @Test
    void currentActZeroOmitsCurrentActSection() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 0, "", List.of());
        assertFalse(prompt.system().contains("CURRENT ACT"));
    }

    @Test
    void actTitleIsNeverSentToTheLlm() {
        ChatScenario scenarioWithTitledAct = scenarioWith("A grumpy innkeeper.", "A stormy night at the inn.",
            List.of(ScenarioAct.leaf(ActNumber.of(1), "Should never reach the LLM", "Act body.")));

        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithTitledAct, sceneFor(scenarioWithTitledAct), 1, "", List.of());

        assertFalse(prompt.system().contains("Should never reach the LLM"));
        assertTrue(prompt.system().contains("Act body."));
    }

    @Test
    void currentActIncludesOnlyThatActsTextNotTheOthers() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithActs, sceneFor(scenarioWithActs), 2, "", List.of());
        assertTrue(prompt.system().contains("CURRENT ACT (2 of 3)"));
        assertTrue(prompt.system().contains("Act two: a stranger arrives."));
        assertFalse(prompt.system().contains("Act one: the storm begins."));
        assertFalse(prompt.system().contains("Act three: the truth comes out."));
    }

    @Test
    void nextActRuleIsOfferedWhenMoreActsRemain() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithActs, sceneFor(scenarioWithActs), 1, "", List.of());
        assertTrue(prompt.system().contains("[NEXT ACT]"));
    }

    @Test
    void nextActRuleIsOmittedOnTheLastAct() {
        ChatPrompt prompt = ChatPromptBuilder.build(scenarioWithActs, sceneFor(scenarioWithActs), 3, "", List.of());
        assertFalse(prompt.system().contains("[NEXT ACT]"));
    }

    @Test
    void defaultNextActRuleIsUsedWhenActTextHasNoBracketedMarker() {
        // "next act" apparait en toutes lettres dans une phrase anodine, sans les crochets du
        // marqueur reel — ne doit PAS declencher la regle conditionnelle (detection par sous-chaine
        // libre autrefois trop permissive, voir revue Fable).
        ChatScenario scenario = scenarioWith("sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "In the next act of her plan, she reveals nothing."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 1, "", List.of());

        assertTrue(prompt.system().contains("you feel this act's events have reached their natural conclusion"));
        assertFalse(prompt.system().contains("tells you exactly when to write"));
    }

    @Test
    void conditionalNextActRuleIsUsedWhenActTextHasTheBracketedMarker() {
        ChatScenario scenario = scenarioWith("sheet", "premise", List.of(
            ScenarioAct.leaf(ActNumber.of(1), "", "Quand écrire [NEXT ACT] : quand la porte s'ouvre."),
            ScenarioAct.leaf(ActNumber.of(2), "", "Act two.")));

        ChatPrompt prompt = ChatPromptBuilder.build(scenario, sceneFor(scenario), 1, "", List.of());

        assertTrue(prompt.system().contains("tells you exactly when to write"));
        assertFalse(prompt.system().contains("you feel this act's events have reached their natural conclusion"));
    }
}
