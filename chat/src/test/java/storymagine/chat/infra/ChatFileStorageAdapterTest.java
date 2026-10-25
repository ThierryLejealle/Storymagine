package storymagine.chat.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storymagine.chat.coeur.domaine.scenario.ChatScenario;
import storymagine.chat.coeur.domaine.session.ChatSession;
import storymagine.chat.coeur.domaine.session.ChatTurn;
import storymagine.chat.coeur.domaine.session.Compaction;
import storymagine.chat.coeur.domaine.session.SavePoint;
import storymagine.chat.coeur.domaine.session.TurnNumber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFileStorageAdapterTest {

    private final ChatFileStorageAdapter adapter = new ChatFileStorageAdapter();

    // ── Le scenario : scenario.txt + characters/ ────────────────────────────

    @Test
    void listScenarioNamesOnlyKeepsDirsWithBothFiles(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "complete", "sheet", "premise");
        Files.createDirectories(root.resolve("incomplete/characters"));
        Files.writeString(root.resolve("incomplete/characters/character.txt"), "# Character\nsheet only", StandardCharsets.UTF_8);

        assertEquals(List.of("complete"), adapter.listScenarioNames(root));
    }

    @Test
    void listScenarioNamesAlsoKeepsAScenarioStillInTheOldLayoutSoChoosingItExplainsTheMigration(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("old"));
        Files.writeString(root.resolve("old/scenario.txt"), "premise", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("old/character.txt"), "# Character\nsheet", StandardCharsets.UTF_8);

        assertEquals(List.of("old"), adapter.listScenarioNames(root));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> adapter.loadScenario(root, "old"));
        assertTrue(e.getMessage().contains("migration.bat"));
    }

    @Test
    void loadScenarioRefusesADirectoryWithOldSessionFilesNextToTheScenario(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        Files.writeString(root.resolve("inn/history.md"), "### PLAYER\nHi.\n", StandardCharsets.UTF_8);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> adapter.loadScenario(root, "inn"));
        assertTrue(e.getMessage().contains("migration.bat"));
    }

    @Test
    void loadScenarioRefusesADirectoryWithAnOldGeneratedDirectoryNextToTheScenario(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        Files.createDirectories(root.resolve("inn/saves"));

        assertThrows(IllegalStateException.class, () -> adapter.loadScenario(root, "inn"));
    }

    @Test
    void loadScenarioReadsBothFilesVerbatim(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "A grumpy innkeeper.", "A stormy night.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("inn", scenario.name());
        assertEquals(1, scenario.cast().npcs().size());
        assertEquals("# Character\nA grumpy innkeeper.", scenario.cast().npcs().get(0).publicInfo());
        assertEquals("A stormy night.", scenario.premise());
        assertTrue(scenario.acts().isEmpty());
        assertEquals("Alex", scenario.playerName(), "pas de ligne \"Joueur : ...\" dans ce fixture, retombe sur le defaut");
    }

    @Test
    void loadScenarioReadsAnOptionalPlayerNameFirstLine(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "Joueur : Kael\nA stormy night.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("Kael", scenario.playerName());
        assertEquals("A stormy night.", scenario.premise(), "la ligne Joueur est retiree, jamais melangee a la premisse");
    }

    @Test
    void loadScenarioReadsThePlayerNameFirstLineAheadOfAScenarioHeading(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "Joueur : Kael\n#SCENARIO\nA stormy night.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("Kael", scenario.playerName());
        assertEquals("A stormy night.", scenario.premise());
    }

    @Test
    void loadScenarioReadsActHeadingsInDocumentOrder(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(2, scenario.acts().size());
        assertEquals("First act.", scenario.acts().get(0).text());
        assertEquals("Second act.", scenario.acts().get(1).text());
    }

    @Test
    void loadScenarioTakesTheActTitleFromItsHeadingLine(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendRawToScenario(root, "inn", "# The searched house\n[The door creaks.]\n\nBody text.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("The searched house", scenario.acts().get(0).title());
        assertEquals("[The door creaks.]\n\nBody text.", scenario.acts().get(0).text());
    }

    @Test
    void loadScenarioLeavesTitleEmptyWhenHeadingHasNoTitleText(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendRawToScenario(root, "inn", "# \nBody text with no title.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("", scenario.acts().get(0).title());
        assertEquals("Body text with no title.", scenario.acts().get(0).text());
    }

    // ── Scenario multi-fichiers (scenario-N.txt) ────────────────────────────

    @Test
    void loadScenarioAppendsContinuationFilesInNumericOrder(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.");
        Files.writeString(root.resolve("inn/scenario-1.txt"), "# Second\nSecond act.", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("inn/scenario-2.txt"), "# Third\nThird act.", StandardCharsets.UTF_8);

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(3, scenario.acts().size());
        assertEquals("First act.", scenario.acts().get(0).text());
        assertEquals("Second act.", scenario.acts().get(1).text());
        assertEquals("Third act.", scenario.acts().get(2).text());
    }

    @Test
    void loadScenarioWithNoContinuationFilesBehavesExactlyAsBefore(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "Only act.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(1, scenario.acts().size());
        assertEquals("Only act.", scenario.acts().get(0).text());
    }

    @Test
    void loadScenarioStopsAtTheFirstMissingContinuationNumber(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.");
        // scenario-1.txt manquant : scenario-2.txt ne doit jamais etre pris en compte.
        Files.writeString(root.resolve("inn/scenario-2.txt"), "# Orphan\nOrphan act.", StandardCharsets.UTF_8);

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(1, scenario.acts().size());
        assertEquals("First act.", scenario.acts().get(0).text());
    }

    @Test
    void loadScenarioRejectsAContinuationFileNotStartingOnATopLevelHeading(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.");
        Files.writeString(root.resolve("inn/scenario-1.txt"), "## Not top-level\nBody.", StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> adapter.loadScenario(root, "inn"));
    }

    @Test
    void aContinuationFileAtTheRootIsNeverTakenForAnOldStyleCharacterSheet(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.");
        // "# Second" ressemble a un en-tete "# Nom" de fiche personnage : ni PNJ, ni declencheur de l'ancien format.
        Files.writeString(root.resolve("inn/scenario-1.txt"), "# Second\nSecond act.", StandardCharsets.UTF_8);

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(1, scenario.cast().npcs().size());
        assertTrue(scenario.cast().find("scenario-1").isEmpty());
    }

    // ── Cast : characters/ ──────────────────────────────────────────────────

    @Test
    void loadScenarioBuildsACastFromEveryTxtFileOfTheCharactersDirectory(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "A grumpy innkeeper.", "A stormy night.");
        writeCharacter(root, "inn", "marcus.txt", "# Marcus\nA retired soldier.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(2, scenario.cast().npcs().size());
        assertTrue(scenario.cast().find("character").isPresent());
        assertEquals("Marcus", scenario.cast().find("marcus").orElseThrow().name());
    }

    @Test
    void loadScenarioIgnoresATxtFileWithNoNameHeadingInsteadOfLoadingItAsANamelessNpc(@TempDir Path root) throws IOException {
        // Un fichier .txt egare dans characters/ (note, brouillon...) ne doit jamais devenir un PNJ
        // fantome silencieux — seuls les fichiers commencant par "# Nom" comptent.
        writeScenarioFiles(root, "inn", "A grumpy innkeeper.", "A stormy night.");
        writeCharacter(root, "inn", "marcus.txt", "# Marcus\nA retired soldier.");
        writeCharacter(root, "inn", "notes.txt", "Idees en vrac pour la suite, pas un perso.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(2, scenario.cast().npcs().size(), "notes.txt n'a pas de \"# Nom\" en premiere ligne, ignore");
        assertTrue(scenario.cast().find("notes").isEmpty());
    }

    @Test
    void loadScenarioNeverReadsACharacterFromOutsideTheCharactersDirectory(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "A grumpy innkeeper.", "A stormy night.");
        // un .txt sans "# Nom" a la racine n'est ni un PNJ, ni un ancien format : simplement ignore
        Files.writeString(root.resolve("inn/notes.txt"), "Idees en vrac.", StandardCharsets.UTF_8);

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(1, scenario.cast().npcs().size());
    }

    @Test
    void loadScenarioSplitsPublicAndSecretInfoOnTheSecretMarker(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        writeCharacter(root, "inn", "elena.txt", "# Elena\nA sharp-tongued merchant.\n\n# SECRET\nSecretly a smuggler.");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        var elena = scenario.cast().find("elena").orElseThrow();
        // Le "# Name" reste dans publicInfo, verbatim (le modele doit encore pouvoir le lire) —
        // seul le contenu APRES "# SECRET" est isole.
        assertEquals("# Elena\nA sharp-tongued merchant.", elena.publicInfo());
        assertEquals("Secretly a smuggler.", elena.secretInfo());
    }

    @Test
    void loadScenarioLeavesSecretInfoEmptyWhenThereIsNoSecretMarker(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "A grumpy innkeeper.", "premise");

        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals("", scenario.cast().npcs().get(0).secretInfo());
    }

    // ── Le run : ouverture d'une partie ─────────────────────────────────────

    @Test
    void loadSessionWithNoRunYetIsEmptyForAnActLessScenarioWithNoBracketedBeats(@TempDir Path root) throws IOException {
        // "premise" n'a aucune ligne "[...]" et ce scenario n'a pas d'actes : aucune intro a
        // generer, la session est bien vide — cas de base, distinct du suivant.
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = adapter.loadSession(root, scenario);

        assertTrue(session.turns().isEmpty());
        assertEquals("", session.summary());
        assertEquals(0, session.currentAct());
    }

    @Test
    void loadSessionWithNoRunYetStillGetsTheOpeningActsBeats(@TempDir Path root) throws IOException {
        // Regression : un scenario jamais joue chargeait une session creuse, sans les lignes "[...]"
        // de l'acte 1 — le joueur n'avait aucune introduction. loadSession() doit produire la meme
        // intro que ChatSession.fresh() directement.
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "[The door creaks open.]\n\nBody text.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = adapter.loadSession(root, scenario);

        assertEquals(1, session.currentAct());
        assertEquals(List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open.")), session.turns());
    }

    @Test
    void loadSessionWithActsAndNoPersistedStateDefaultsToActOne(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = adapter.loadSession(root, scenario);

        assertEquals(1, session.currentAct());
    }

    // ── Y a-t-il une partie a reprendre ? ───────────────────────────────────

    @Test
    void thereIsNothingToResumeForAScenarioNeverPlayed(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");

        assertFalse(adapter.hasPlayedTurns(root, adapter.loadScenario(root, "inn")));
    }

    @Test
    void aRunHoldingOnlyTheNarratorsOpeningLinesIsNotAGameToResume(@TempDir Path root) throws IOException {
        // c'est ce qu'ecrit un demarrage ou un "Recommencer" : une intro, aucune partie jouee
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "[The door creaks open.]\n\nBody text.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, ChatSession.fresh(scenario));

        assertFalse(adapter.hasPlayedTurns(root, scenario));
    }

    @Test
    void aRunWithAPlayerTurnIsAGameToResume(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.NARRATOR, "Intro.")), "", 0);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hello."));
        adapter.saveSession(root, session);

        assertTrue(adapter.hasPlayedTurns(root, scenario));
    }

    @Test
    void nothingIsLeftToResumeOnceTheRunIsArchivedByAReset(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));
        assertTrue(adapter.hasPlayedTurns(root, scenario));

        adapter.resetSession(root, scenario);

        assertFalse(adapter.hasPlayedTurns(root, scenario));
    }

    // ── Le run : un fichier par tour ────────────────────────────────────────

    @Test
    void saveWritesOneFilePerTurnWithItsNumberAndTheActItWasWrittenIn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(), "", 1);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hello."));
        session.append(new ChatTurn(ChatTurn.Speaker.LLM, "*nods*\n\nWelcome.", "", "character"));
        session.advanceAct();
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Thanks."));

        adapter.saveSession(root, session);

        assertEquals(List.of("000001.md", "000002.md", "000003.md"), turnFiles(root, "inn"));
        assertEquals("----- #2 · acte 1 -----\n### LLM: character\n*nods*\n\nWelcome.\n", readTurn(root, "inn", "000002.md"));
        assertTrue(readTurn(root, "inn", "000003.md").startsWith("----- #3 · acte 2 -----\n### PLAYER\n"));
    }

    @Test
    void saveThenLoadRoundTripsMultilineTurnsNumbersAndCurrentAct(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        // Un tour avec des lignes vides internes — le cas qui casse un parsing naif.
        ChatSession session = new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "*enters*\n\nHello there."),
            new ChatTurn(ChatTurn.Speaker.LLM, "*grunts*\nWhat do you want?")
        ), "", 2);

        adapter.saveSession(root, session);
        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals(session.turns(), reloaded.turns());
        assertEquals(TurnNumber.of(1), reloaded.firstTurnNumber());
        assertEquals(TurnNumber.of(2), reloaded.lastTurnNumber());
        assertEquals(2, reloaded.currentAct());
    }

    @Test
    void saveThenLoadRoundTripsNarratorTurns(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.NARRATOR, "The door creaks open."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello.")
        ), "", 0);

        adapter.saveSession(root, session);

        assertEquals(session.turns(), adapter.loadSession(root, scenario).turns());
    }

    @Test
    void saveThenLoadRoundTripsWhichNpcSpokeEachLlmTurn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        writeCharacter(root, "inn", "marcus.txt", "# Marcus\nsoldier");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi both."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Hello.", "", "character"),
            new ChatTurn(ChatTurn.Speaker.LLM, "Greetings.", "", "marcus")
        ), "", 0);

        adapter.saveSession(root, session);
        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals(session.turns(), reloaded.turns());
        assertEquals("character", reloaded.turns().get(1).npcId());
        assertEquals("marcus", reloaded.turns().get(2).npcId());
    }

    @Test
    void saveSessionDeletesTheFilesOfTurnsThatNoLongerExistInTheSession(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "One."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Two."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Three.")
        ), "", 0);
        adapter.saveSession(root, session);

        session.truncateFrom(1);
        adapter.saveSession(root, session);

        assertEquals(List.of("000001.md"), turnFiles(root, "inn"));
    }

    @Test
    void anUnreadableTurnFileFailsLoudlyInsteadOfBeingSilentlySkipped(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "One.")), "", 0));
        Files.writeString(root.resolve("inn/run/current/turns/000001.md"), "not a turn file", StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class, () -> adapter.loadSession(root, scenario));
    }

    @Test
    void aMissingTurnFileInsideTheWindowFailsLoudly(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));
        Files.delete(root.resolve("inn/run/current/turns/000002.md"));

        assertThrows(IllegalStateException.class, () -> adapter.loadSession(root, scenario));
    }

    // ── Le run : compactions ────────────────────────────────────────────────

    @Test
    void aCompactionIsItsOwnFileNamedAfterWhereItWasMadeAndWhatItFolded(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = sixTurnSession(scenario);
        Compaction compaction = session.compactInto("They met at the door.", session.turns().subList(4, 6));

        adapter.writeCompaction(root, scenario, compaction);

        assertEquals(new Compaction(TurnNumber.of(6), TurnNumber.of(4), "They met at the door."), compaction);
        assertEquals(List.of("000006-compaction-f000004.md"), turnFiles(root, "inn"));
        assertEquals("They met at the door.", readTurn(root, "inn", "000006-compaction-f000004.md"));
    }

    @Test
    void loadSessionAfterACompactionRestoresTheSummaryAndOnlyTheVerbatimWindow(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = sixTurnSession(scenario);
        adapter.saveSession(root, session);
        adapter.writeCompaction(root, scenario, session.compactInto("Folded.", session.turns().subList(4, 6)));
        adapter.saveSession(root, session);

        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals("Folded.", reloaded.summary());
        assertEquals(TurnNumber.of(5), reloaded.firstTurnNumber());
        assertEquals(TurnNumber.of(6), reloaded.lastTurnNumber());
        assertEquals(session.turns(), reloaded.turns());
        // les tours replies restent sur disque, un fichier chacun : la compaction ne supprime rien
        assertEquals(7, turnFiles(root, "inn").size());
    }

    /** Joue 8 tours avec une compaction apres le 6 (repliant jusqu'au 4) puis une apres le 8 (repliant jusqu'au 6). */
    private ChatSession runWithTwoCompactions(Path root, ChatScenario scenario) {
        ChatSession session = sixTurnSession(scenario);
        adapter.saveSession(root, session);
        adapter.writeCompaction(root, scenario, session.compactInto("Summary A.", session.turns().subList(4, 6)));
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Seven."));
        session.append(new ChatTurn(ChatTurn.Speaker.LLM, "Eight."));
        adapter.saveSession(root, session);
        adapter.writeCompaction(root, scenario, session.compactInto("Summary B.", session.turns().subList(2, 4)));
        adapter.saveSession(root, session);
        return session;
    }

    @Test
    void loadSessionAtPicksTheMostRecentCompactionMadeAtOrBeforeThatTurn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        runWithTwoCompactions(root, scenario);

        ChatSession at8 = adapter.loadSessionAt(root, scenario, TurnNumber.of(8));
        assertEquals("Summary B.", at8.summary());
        assertEquals(TurnNumber.of(7), at8.firstTurnNumber());

        // la compaction B (faite apres le tour 8) n'existait pas encore au tour 7 : c'est A qui s'applique
        ChatSession at7 = adapter.loadSessionAt(root, scenario, TurnNumber.of(7));
        assertEquals("Summary A.", at7.summary());
        assertEquals(TurnNumber.of(5), at7.firstTurnNumber());
        assertEquals(TurnNumber.of(7), at7.lastTurnNumber());

        // une compaction faite APRES le tour N s'applique deja au tour N lui-meme
        ChatSession at6 = adapter.loadSessionAt(root, scenario, TurnNumber.of(6));
        assertEquals("Summary A.", at6.summary());
        assertEquals(List.of("Five.", "Six."), at6.turns().stream().map(ChatTurn::text).toList());

        // avant la premiere compaction : pas de resume, tous les tours depuis le debut
        ChatSession at5 = adapter.loadSessionAt(root, scenario, TurnNumber.of(5));
        assertEquals("", at5.summary());
        assertEquals(TurnNumber.of(1), at5.firstTurnNumber());
        assertEquals(5, at5.turns().size());
    }

    @Test
    void loadSessionAtUsesTheActWrittenOnThatTurnNotTheCurrentOne(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(), "", 1);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "One."));
        session.advanceAct();
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Two."));
        adapter.saveSession(root, session);

        assertEquals(1, adapter.loadSessionAt(root, scenario, TurnNumber.of(1)).currentAct());
        assertEquals(2, adapter.loadSessionAt(root, scenario, TurnNumber.of(2)).currentAct());
    }

    @Test
    void loadSessionAtBeforeTheFirstTurnIsAnEmptyWindow(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));

        ChatSession atZero = adapter.loadSessionAt(root, scenario, TurnNumber.NONE);

        assertTrue(atZero.turns().isEmpty());
        assertEquals(TurnNumber.NONE, atZero.lastTurnNumber());
    }

    @Test
    void loadSessionAtRejectsATurnBeyondTheLastOne(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));

        assertThrows(IllegalArgumentException.class, () -> adapter.loadSessionAt(root, scenario, TurnNumber.of(4)));
    }

    @Test
    void loadSessionAtNeverModifiesTheRunOnDisk(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        runWithTwoCompactions(root, scenario);
        List<String> before = turnFiles(root, "inn");

        adapter.loadSessionAt(root, scenario, TurnNumber.of(3));

        assertEquals(before, turnFiles(root, "inn"));
    }

    @Test
    void truncateAfterDeletesLaterTurnsAndLaterCompactionsButKeepsOneMadeRightAfterThatTurn(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        runWithTwoCompactions(root, scenario);

        adapter.truncateAfter(root, scenario, TurnNumber.of(6));

        List<String> files = turnFiles(root, "inn");
        assertTrue(files.contains("000006.md"));
        assertTrue(files.contains("000006-compaction-f000004.md"), "faite apres le tour 6 : elle fait partie de l'etat au tour 6");
        assertFalse(files.contains("000007.md"));
        assertFalse(files.contains("000008.md"));
        assertFalse(files.contains("000008-compaction-f000006.md"));
    }

    @Test
    void anUnrelatedFileDroppedInTheTurnsDirectoryIsIgnored(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));
        Files.writeString(root.resolve("inn/run/current/turns/notes.txt"), "stray", StandardCharsets.UTF_8);

        assertEquals(3, adapter.loadSession(root, scenario).turns().size());
    }

    // ── Le run : presence, interjection, acte courant (state.txt) ───────────

    @Test
    void saveThenLoadRoundTripsPresentNpcIds(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        writeCharacter(root, "inn", "marcus.txt", "# Marcus\nsoldier");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(), "", 0, Set.of("marcus"));

        adapter.saveSession(root, session);
        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals(Set.of("marcus"), reloaded.presentNpcIds());
    }

    @Test
    void loadSessionWithNoStateFileDefaultsToEveryCastMemberPresent(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        writeCharacter(root, "inn", "marcus.txt", "# Marcus\nsoldier");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        ChatSession session = adapter.loadSession(root, scenario);

        assertEquals(Set.of("character", "marcus"), session.presentNpcIds());
    }

    @Test
    void saveThenLoadRoundTripsInterjectingNpcIds(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        writeCharacter(root, "inn", "marcus.txt", "# Marcus\nsoldier");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(), "", 0, Set.of("character", "marcus"), Set.of("marcus"));

        adapter.saveSession(root, session);
        ChatSession reloaded = adapter.loadSession(root, scenario);

        assertEquals(Set.of("marcus"), reloaded.interjectingNpcIds());
    }

    @Test
    void theActInStateTxtWinsOnLoadSoAManualPreviousActIsNotLost(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(), "", 2);
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi."));
        session.previousAct(); // aucun tour n'a ete ecrit depuis : seul state.txt le sait
        adapter.saveSession(root, session);

        assertEquals(1, adapter.loadSession(root, scenario).currentAct());
        assertEquals(2, adapter.loadSessionAt(root, scenario, TurnNumber.of(1)).currentAct(), "a un tour donne, c'est l'acte ecrit sur ce tour");
    }

    // ── Archive au redemarrage ──────────────────────────────────────────────

    @Test
    void resetSessionArchivesTheWholeRunAsAZipInsteadOfDeletingItButKeepsTheScenario(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));

        adapter.resetSession(root, scenario);

        assertFalse(Files.exists(root.resolve("inn/run/current")));
        assertTrue(Files.exists(root.resolve("inn/scenario.txt")));
        assertTrue(Files.exists(root.resolve("inn/characters/character.txt")));
        assertTrue(adapter.loadSession(root, scenario).turns().isEmpty());
        // pas de perte : tout l'historique est dans un zip d'archive
        try (var archived = Files.list(root.resolve("inn/run/archive"))) {
            List<Path> zips = archived.toList();
            assertEquals(1, zips.size());
            assertTrue(zips.get(0).getFileName().toString().endsWith(".zip"));
            assertEquals(3, RunArchiver.lastTurnNumber(zips.get(0)));
        }
    }

    @Test
    void resetSessionKeepsTheTurnsOfAnArchivedRunOutOfTheNextRun(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        adapter.saveSession(root, threeTurnSession(scenario));
        adapter.resetSession(root, scenario);

        ChatSession next = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "New game.")), "", 0);
        adapter.saveSession(root, next);

        assertEquals(List.of("000001.md"), turnFiles(root, "inn"), "la nouvelle partie repart de 1, sans rien de l'ancienne");
    }

    @Test
    void resetSessionNeverTouchesSavePoints(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Hi.")), "", 0);
        adapter.saveSession(root, session);
        SavePoint save = adapter.createSavePoint(root, session);

        adapter.resetSession(root, scenario);

        assertEquals(List.of(save), adapter.listSavePoints(root, scenario));
    }

    // ── Sauvegardes manuelles : un zip de current/ ──────────────────────────

    @Test
    void createSavePointThenLoadSavePointRestoresTheWholeRunHistoryIncluded(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        appendActs(root, "inn", "First act.", "Second act.");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = sixTurnSession(scenario);
        adapter.saveSession(root, session);
        adapter.writeCompaction(root, scenario, session.compactInto("Summary so far.", session.turns().subList(4, 6)));
        adapter.saveSession(root, session);
        SavePoint save = adapter.createSavePoint(root, session);

        // la partie continue et diverge apres la sauvegarde
        session.append(new ChatTurn(ChatTurn.Speaker.PLAYER, "Seven."));
        adapter.saveSession(root, session);
        ChatSession loaded = adapter.loadSavePoint(root, scenario, save.id());

        assertEquals(6, save.lastTurnNumber());
        assertEquals("Summary so far.", loaded.summary());
        assertEquals(TurnNumber.of(6), loaded.lastTurnNumber());
        assertEquals(List.of("Five.", "Six."), loaded.turns().stream().map(ChatTurn::text).toList());
        // l'historique complet est revenu : on peut encore remonter avant la compaction
        assertEquals("", adapter.loadSessionAt(root, scenario, TurnNumber.of(3)).summary());
        assertFalse(turnFiles(root, "inn").contains("000007.md"), "le tour joue apres la sauvegarde n'existe plus");
    }

    @Test
    void createSavePointNeverTouchesTheLiveRun(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession live = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "Live.")), "", 0);
        adapter.saveSession(root, live);

        adapter.createSavePoint(root, live);

        assertEquals(live.turns(), adapter.loadSession(root, scenario).turns());
    }

    @Test
    void aSavePointIsASingleZipFile(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = threeTurnSession(scenario);
        adapter.saveSession(root, session);

        SavePoint save = adapter.createSavePoint(root, session);

        assertTrue(Files.isRegularFile(root.resolve("inn/run/saves/" + save.id() + ".zip")));
    }

    @Test
    void listSavePointsReturnsNewestFirstWithTheirLastTurnNumber(@TempDir Path root) throws IOException, InterruptedException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        ChatSession session = new ChatSession(scenario, List.of(new ChatTurn(ChatTurn.Speaker.PLAYER, "One.")), "", 0);
        adapter.saveSession(root, session);
        SavePoint first = adapter.createSavePoint(root, session);
        Thread.sleep(5);
        session.append(new ChatTurn(ChatTurn.Speaker.LLM, "Two."));
        adapter.saveSession(root, session);
        SavePoint second = adapter.createSavePoint(root, session);

        assertEquals(List.of(second, first), adapter.listSavePoints(root, scenario));
        assertEquals(2, adapter.listSavePoints(root, scenario).get(0).lastTurnNumber());
        assertEquals(1, adapter.listSavePoints(root, scenario).get(1).lastTurnNumber());
    }

    @Test
    void listSavePointsIsEmptyWhenNoneWereEverCreated(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertEquals(List.of(), adapter.listSavePoints(root, scenario));
    }

    @Test
    void loadSavePointRejectsAnIdThatIsNotAPlainTimestamp(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertThrows(IllegalArgumentException.class,
            () -> adapter.loadSavePoint(root, scenario, "../../etc/passwd"));
    }

    @Test
    void loadSavePointRejectsAnUnknownId(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertThrows(IllegalArgumentException.class,
            () -> adapter.loadSavePoint(root, scenario, "2026-01-01-000000-000"));
    }

    @Test
    void deleteSavePointRemovesItFromTheListing(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");
        SavePoint save = adapter.createSavePoint(root, new ChatSession(scenario, List.of(), "", 0));

        adapter.deleteSavePoint(root, scenario, save.id());

        assertEquals(List.of(), adapter.listSavePoints(root, scenario));
    }

    @Test
    void deleteSavePointIsANoOpWhenTheIdDoesNotExist(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        adapter.deleteSavePoint(root, scenario, "2026-01-01-000000-000");

        assertEquals(List.of(), adapter.listSavePoints(root, scenario));
    }

    @Test
    void deleteSavePointRejectsAnIdThatIsNotAPlainTimestamp(@TempDir Path root) throws IOException {
        writeScenarioFiles(root, "inn", "sheet", "premise");
        ChatScenario scenario = adapter.loadScenario(root, "inn");

        assertThrows(IllegalArgumentException.class,
            () -> adapter.deleteSavePoint(root, scenario, "../../etc/passwd"));
    }

    // ── Fixtures ────────────────────────────────────────────────────────────

    private static ChatSession threeTurnSession(ChatScenario scenario) {
        return new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "One."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Two."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Three.")
        ), "", 0);
    }

    private static ChatSession sixTurnSession(ChatScenario scenario) {
        return new ChatSession(scenario, List.of(
            new ChatTurn(ChatTurn.Speaker.PLAYER, "One."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Two."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Three."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Four."),
            new ChatTurn(ChatTurn.Speaker.PLAYER, "Five."),
            new ChatTurn(ChatTurn.Speaker.LLM, "Six.")
        ), "", 0);
    }

    /** Names of every file of the scenario's run/current/turns/ directory, sorted. */
    private static List<String> turnFiles(Path root, String name) throws IOException {
        Path dir = root.resolve(name).resolve("run/current/turns");
        if (!Files.isDirectory(dir)) return List.of();
        try (var files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    private static String readTurn(Path root, String name, String fileName) throws IOException {
        return Files.readString(root.resolve(name).resolve("run/current/turns").resolve(fileName), StandardCharsets.UTF_8);
    }

    /** character is the sheet BODY only — "# Character" is prepended, required for a .txt file to load as an Npc. */
    private static void writeScenarioFiles(Path root, String name, String character, String scenarioText) throws IOException {
        Path dir = root.resolve(name);
        Files.createDirectories(dir.resolve("characters"));
        Files.writeString(dir.resolve("characters/character.txt"), "# Character\n" + character, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scenario.txt"), scenarioText, StandardCharsets.UTF_8);
    }

    private static void writeCharacter(Path root, String name, String fileName, String content) throws IOException {
        Files.writeString(root.resolve(name).resolve("characters").resolve(fileName), content, StandardCharsets.UTF_8);
    }

    /** Appends flat, top-level "# " act headings to the scenario's already-written scenario.txt. */
    private static void appendActs(Path root, String name, String... actBodies) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String body : actBodies) sb.append("\n\n# \n").append(body);
        appendRawToScenario(root, name, sb.toString());
    }

    private static void appendRawToScenario(Path root, String name, String raw) throws IOException {
        Path file = root.resolve(name).resolve("scenario.txt");
        Files.writeString(file, Files.readString(file, StandardCharsets.UTF_8) + "\n\n" + raw, StandardCharsets.UTF_8);
    }
}
