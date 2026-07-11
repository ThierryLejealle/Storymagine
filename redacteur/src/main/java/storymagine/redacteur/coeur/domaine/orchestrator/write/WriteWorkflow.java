package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.commun.coeur.domaine.text.TextPatcher;
import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrectorFinding;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacritic.DeusInMachinaCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic.CheckCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic.PlanFidelityCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic.ChapterGoalCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.ChapterCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic.ChapterDreamCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic.ChapterNarrativeCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic.ChapterWhatIfCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector.NaturalityFinding;
import storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector.NaturalityCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.commun.RetryStrategy;
import storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector.PhrasingCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector.GrammarCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.repetitionfilter.RepetitionFilterOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.repetitiontracker.RepetitionTrackerOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.stateextractor.StateExtractorOutput;
import storymagine.redacteur.coeur.domaine.agent.sequence.stylecorrector.StyleCorrectorFinding;
import storymagine.redacteur.coeur.domaine.agent.sequence.stylecorrector.StyleCorrectorOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterGoalCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterDreamCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterWhatIfCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.common.ScenarioFormatters;
import storymagine.redacteur.coeur.domaine.orchestrator.common.StoryFormatters;
import storymagine.redacteur.coeur.domaine.scenario.Chapter;
import storymagine.redacteur.coeur.domaine.scenario.NarrativeType;
import storymagine.redacteur.coeur.domaine.scenario.Scenario;
import storymagine.redacteur.coeur.domaine.scenario.Sequence;
import storymagine.redacteur.coeur.domaine.story.Story;
import storymagine.redacteur.coeur.domaine.story.WorldState;
import storymagine.redacteur.coeur.domaine.story.WrittenChapter;
import storymagine.redacteur.coeur.domaine.story.WrittenSequence;
import storymagine.redacteur.coeur.ports.HtmlExportPort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the writing phase for one chapter.
 *
 * Per sequence:
 *   PHASE 1 — WRITE
 *     Writer
 *
 *   PHASE 2 — CORRECTORS  (skip if BROUILLON)
 *     PhrasingCorrector → DeusInMachinaCorrector → NaturalityCorrector → StyleCorrector → GrammarCorrector
 *     Patches applied inline via String.replace — no Writer retry.
 *
 *   PHASE 3 — GLOBAL CRITIQUE  (skip if BROUILLON)
 *     All critics run together: DeusInMachinaCritic, StyleCritic, PlanFidelityCritic (if beats), CheckCritic (if checks).
 *     Global average score computed from all critics.
 *     If score < SEQUENCE_CRITIC_THRESHOLD, OR any single critic score < QualityLevel.eliminationThreshold(),
 *     AND retry budget remains:
 *       → Writer (ALL collected problems as feedback) → Phase 2 again → Critics again
 *     Best version (highest average score) is retained across passes.
 *
 *   PHASE 4 — POST-PROCESSING  (always)
 *     StateExtractor → RepetitionTracker → RepetitionFilter
 *
 * Chapter level (after all sequences, STANDARD / FULL modes):
 *   Chapter critics run together on full text → global average.
 *   If avg <= chapitreThreshold, OR any single critic score < QualityLevel.eliminationThreshold(),
 *   AND retry budget remains: REBOOT all sequences with feedback.
 *   Best chapter version (highest average) is retained.
 *
 * DREAM / WHAT_IF chapters use snapshot/restore on WorldState.
 */
public class WriteWorkflow {

    private static final int SEQUENCE_CRITIC_THRESHOLD = 7;

    // Phase 1
    private final WriterStep                 writerStep;

    // Phase 2 — Correctors (ordered)
    private final PhrasingCorrectorStep      phrasingCorrectorStep;
    private final DeusInMachinaCorrectorStep deusInMachinaCorrectorStep;
    private final NaturalityCorrectorStep    naturalityCorrectorStep;
    private final GrammarCorrectorStep       grammarCorrectorStep;

    // Phase 2 — Correctors (ordered) — continued
    private final StyleCorrectorStep         styleCorrectorStep;

    // Phase 3 — Critics (run together)
    private final DeusInMachinaCriticStep    deusInMachinaCriticStep;
    private final PlanFidelityCriticStep     planFidelityCriticStep;
    private final CheckCriticStep            checkCriticStep;

    // Phase 4 — Post-processing
    private final StateExtractorStep         stateExtractorStep;
    private final RepetitionTrackerStep      repetitionTrackerStep;
    private final RepetitionFilterStep       repetitionFilterStep;

    // Chapter-level critics
    private final ChapterNarrativeCriticStep    textNarrativeCriticStep;
    private final ChapterCoherenceCriticStep    textCoherenceCriticStep;
    private final ChapterDreamCriticStep        textDreamCriticStep;
    private final ChapterWhatIfCriticStep       textWhatIfCriticStep;
    private final ChapterGoalCriticStep         goalTextCriticStep;

    private final CorrectorConfig            correctorConfig;
    private final HtmlExportPort             htmlExport;
    private final LogPort                    log;

    public WriteWorkflow(WriterStep writerStep,
                         PhrasingCorrectorStep phrasingCorrectorStep,
                         DeusInMachinaCorrectorStep deusInMachinaCorrectorStep,
                         NaturalityCorrectorStep naturalityCorrectorStep,
                         GrammarCorrectorStep grammarCorrectorStep,
                         StyleCorrectorStep styleCorrectorStep,
                         DeusInMachinaCriticStep deusInMachinaCriticStep,
                         PlanFidelityCriticStep planFidelityCriticStep,
                         CheckCriticStep checkCriticStep,
                         StateExtractorStep stateExtractorStep,
                         RepetitionTrackerStep repetitionTrackerStep,
                         RepetitionFilterStep repetitionFilterStep,
                         ChapterNarrativeCriticStep textNarrativeCriticStep,
                         ChapterCoherenceCriticStep textCoherenceCriticStep,
                         ChapterDreamCriticStep textDreamCriticStep,
                         ChapterWhatIfCriticStep textWhatIfCriticStep,
                         ChapterGoalCriticStep goalTextCriticStep,
                         CorrectorConfig correctorConfig,
                         HtmlExportPort htmlExport,
                         LogPort log) {
        this.writerStep                 = writerStep;
        this.phrasingCorrectorStep      = phrasingCorrectorStep;
        this.deusInMachinaCorrectorStep = deusInMachinaCorrectorStep;
        this.naturalityCorrectorStep    = naturalityCorrectorStep;
        this.grammarCorrectorStep       = grammarCorrectorStep;
        this.styleCorrectorStep         = styleCorrectorStep;
        this.deusInMachinaCriticStep    = deusInMachinaCriticStep;
        this.planFidelityCriticStep     = planFidelityCriticStep;
        this.checkCriticStep            = checkCriticStep;
        this.stateExtractorStep         = stateExtractorStep;
        this.repetitionTrackerStep      = repetitionTrackerStep;
        this.repetitionFilterStep       = repetitionFilterStep;
        this.textNarrativeCriticStep    = textNarrativeCriticStep;
        this.textCoherenceCriticStep    = textCoherenceCriticStep;
        this.textDreamCriticStep        = textDreamCriticStep;
        this.textWhatIfCriticStep       = textWhatIfCriticStep;
        this.goalTextCriticStep         = goalTextCriticStep;
        this.correctorConfig            = correctorConfig;
        this.htmlExport                 = htmlExport;
        this.log                        = log;
    }

    /** Writes all sequences of the current chapter in Story. Mutates Story. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        boolean isolated = chapter.type() != NarrativeType.IMPERATIVE;
        WorldState.Snapshot snapshot = isolated ? story.worldState().snapshot() : null;

        List<Sequence> sequences      = chapter.sequences();
        boolean        runCritique    = config.qualityLevel().runsSequenceCheckers();
        int            chapMaxAttempts = runCritique ? 1 + config.qualityLevel().chapitreMaxRetry() : 1;

        List<WrittenSequence> bestSequences    = null;
        double                bestScore        = -1.0;
        int                   bestChapPass     = 1;
        int                   finalChapPasses  = 0;
        String                chapterProblems  = null;

        for (int chapPass = 0; chapPass < chapMaxAttempts; chapPass++) {
            finalChapPasses = chapPass + 1;
            WrittenChapter wc = story.currentChapter().orElseThrow();
            if (chapPass > 0) wc.clearSequences();

            for (int i = 0; i < sequences.size(); i++) {
                writeSequence(scenario, chapter, sequences.get(i), story, config,
                              i + 1, sequences.size(), chapterProblems);
            }

            if (!runCritique) break;

            log.phaseHeader("EVALUER CHAPITRE", chapter.title());
            ChapterCritiqueResult critique = runChapterCritique(chapter, scenario,
                                                                wc.fullText(), story, config);
            double  elimination   = config.qualityLevel().eliminationThreshold();
            double  minScore      = critique.scores().stream().mapToDouble(Double::doubleValue).min().orElse(10.0);
            boolean eliminated    = minScore < elimination;
            boolean avgPassed     = critique.avg() > config.qualityLevel().chapitreThreshold();
            boolean passed        = avgPassed && !eliminated;
            boolean isLastAttempt = chapPass == chapMaxAttempts - 1;

            if (avgPassed && eliminated)
                log.warn(String.format("Chapitre : note eliminatoire franchie (seuil %.1f) — relance forcee malgre une moyenne suffisante", elimination));

            if (bestSequences == null || critique.avg() > bestScore) {
                bestScore     = critique.avg();
                bestSequences = new ArrayList<>(wc.sequences());
                bestChapPass  = chapPass + 1;
            }

            String hint = (!passed && !isLastAttempt) ? "CHAPITRE " + (chapPass + 2) + "/" + chapMaxAttempts : null;
            log.scoresSummary(critique.avg(), config.qualityLevel().chapitreThreshold(), minScore, elimination, passed, hint);

            if (passed || isLastAttempt) break;

            chapterProblems = critique.problems();
        }

        if (runCritique && bestSequences != null) {
            WrittenChapter wc = story.currentChapter().orElseThrow();
            wc.clearSequences();
            for (WrittenSequence seq : bestSequences) wc.addSequence(seq);
            if (finalChapPasses > 1) log.chapterRetained(bestChapPass, finalChapPasses, bestScore);
        }

        if (isolated) story.worldState().restore(snapshot);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 1 + 2 + 3 + 4 — per sequence
    // ─────────────────────────────────────────────────────────────────────────

    private void writeSequence(Scenario scenario, Chapter chapter, Sequence seq,
                                Story story, GenerationConfig config,
                                int seqIdx, int seqTotal, String chapterRewriteProblems) {
        log.phaseHeader("WRITE " + seqIdx + "/" + seqTotal, seq.type().name().toLowerCase());

        List<String> seqPlans     = story.currentChapter().orElseThrow().sequencePlans();
        String       sequencePlan = (!seqPlans.isEmpty() && seqIdx <= seqPlans.size())
                ? seqPlans.get(seqIdx - 1) : null;

        // Phase 1 — Write
        long t0 = System.nanoTime();
        String text = writerStep.run(scenario, chapter, seq, story, sequencePlan,
                                     chapterRewriteProblems).text();
        log.step("SequenceWriter", ms(t0), "-> " + countWords(text) + " mots");

        // Phase 2 — Correctors
        text = applyCorrectorsPhase(text, scenario, chapter, seq, sequencePlan, config);

        // Phase 3 — Global critique with vote
        if (config.qualityLevel().runsSequenceCheckers()) {
            text = runSequenceCritique(text, scenario, chapter, seq, story, sequencePlan, config);
        }

        // Phase 4 — Post-processing
        t0 = System.nanoTime();
        StateExtractorOutput stateOut = stateExtractorStep.run(text, story.worldState());
        if (stateOut.hasChanges()) StoryFormatters.applyStateLines(story.worldState(), stateOut.stateLines());
        log.step("SequenceStateExtractor", ms(t0),
                 stateOut.hasChanges() ? "-> " + countLines(stateOut.stateLines()) + " changement(s)" : null);

        t0 = System.nanoTime();
        RepetitionTrackerOutput trackOut = repetitionTrackerStep.run(text, story.repetitionMemory());
        log.step("SequenceRepetitionTracker", ms(t0), null);

        t0 = System.nanoTime();
        RepetitionFilterOutput filterOut = repetitionFilterStep.run(
                trackOut.phrases(), ScenarioFormatters.keepPhrases(scenario));
        story.repetitionMemory().addPhrases(filterOut.filteredCandidates());
        story.repetitionMemory().addThemes(trackOut.themes());
        log.step("SequenceRepetitionFilter", ms(t0),
                 filterOut.filteredCandidates().isEmpty() ? null
                         : "-> " + filterOut.filteredCandidates().size() + " filtre(s)");

        story.currentChapter().orElseThrow().addSequence(new WrittenSequence(sequencePlan, text));
        log.sequenceText(chapter.title(), seqIdx, text);
        htmlExport.exportHtml(scenario.config().title(), story);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2 — Correctors
    // ─────────────────────────────────────────────────────────────────────────

    /** Applies all correctors in sequence. Skipped entirely on BROUILLON quality level. */
    private String applyCorrectorsPhase(String text, Scenario scenario, Chapter chapter,
                                         Sequence seq, String sequencePlan, GenerationConfig config) {
        if (!config.qualityLevel().runsProofreader()) return text;

        boolean repeat   = config.qualityLevel().runsCorrectorsRepeat()
                           && correctorConfig.repeatThresholdPerWord() > 0;
        int     maxPasses = correctorConfig.maxRetryPasses();

        // Phrasing
        RetryStrategy phrasingStrategy = phrasingCorrectorStep.retryStrategy();
        int phrasingPrevCount = Integer.MAX_VALUE;
        long tPhrasing0 = System.nanoTime();
        PhrasingCorrectorOutput phrasingOut = phrasingCorrectorStep.run(text);
        text = applyPhrasingCorrections(text, phrasingOut);
        log.step("SequencePhrasingCorrector", ms(tPhrasing0),
                 "-> " + phrasingOut.corrections().size() + " correction(s)");
        if (repeat) {
            for (int pass = 2; pass <= 1 + maxPasses && needsRetry(phrasingStrategy, phrasingOut.corrections().size(), phrasingPrevCount, text); pass++) {
                phrasingPrevCount = phrasingOut.corrections().size();
                tPhrasing0 = System.nanoTime();
                phrasingOut = phrasingCorrectorStep.run(text);
                text = applyPhrasingCorrections(text, phrasingOut);
                log.step("SequencePhrasingCorrector (pass " + pass + ")", ms(tPhrasing0),
                         "-> " + phrasingOut.corrections().size() + " correction(s)");
            }
            if (needsRetry(phrasingStrategy, phrasingOut.corrections().size(), phrasingPrevCount, text))
                log.warn(String.format("PhrasingCorrector: seuil encore depasse apres relance — %.3f corr/mot (seuil %.3f) ou >= %d corrections",
                                       ratio(phrasingOut.corrections().size(), text), correctorConfig.repeatThresholdPerWord(), correctorConfig.minCorrectionsForRetry()));
        }

        // DeusInMachina
        RetryStrategy deusStrategy = deusInMachinaCorrectorStep.retryStrategy();
        int deusPrevCount = Integer.MAX_VALUE;
        long t0 = System.nanoTime();
        DeusInMachinaCorrectorOutput deusOut = deusInMachinaCorrectorStep.run(text, scenario, chapter, seq, sequencePlan);
        text = applyDeusCorrections(text, deusOut.findings());
        log.step("SequenceDeusInMachinaCorrector", ms(t0),
                 "-> " + deusOut.findings().size() + " correction(s)");
        if (repeat) {
            for (int pass = 2; pass <= 1 + maxPasses && needsRetry(deusStrategy, deusOut.findings().size(), deusPrevCount, text); pass++) {
                deusPrevCount = deusOut.findings().size();
                t0 = System.nanoTime();
                deusOut = deusInMachinaCorrectorStep.run(text, scenario, chapter, seq, sequencePlan);
                text = applyDeusCorrections(text, deusOut.findings());
                log.step("SequenceDeusInMachinaCorrector (pass " + pass + ")", ms(t0),
                         "-> " + deusOut.findings().size() + " correction(s)");
            }
            if (needsRetry(deusStrategy, deusOut.findings().size(), deusPrevCount, text))
                log.warn(String.format("DeusInMachinaCorrector: seuil encore depasse apres relance — %.3f corr/mot (seuil %.3f) ou >= %d corrections",
                                       ratio(deusOut.findings().size(), text), correctorConfig.repeatThresholdPerWord(), correctorConfig.minCorrectionsForRetry()));
        }

        // Naturality
        RetryStrategy natStrategy = naturalityCorrectorStep.retryStrategy();
        int natPrevCount = Integer.MAX_VALUE;
        t0 = System.nanoTime();
        NaturalityCorrectorOutput natOut = naturalityCorrectorStep.run(text);
        text = applyNaturalityFixes(text, natOut.findings());
        log.step("SequenceNaturalityCorrector", ms(t0),
                 "-> " + natOut.findings().size() + " correction(s)");
        if (repeat) {
            for (int pass = 2; pass <= 1 + maxPasses && needsRetry(natStrategy, natOut.findings().size(), natPrevCount, text); pass++) {
                natPrevCount = natOut.findings().size();
                t0 = System.nanoTime();
                natOut = naturalityCorrectorStep.run(text);
                text = applyNaturalityFixes(text, natOut.findings());
                log.step("SequenceNaturalityCorrector (pass " + pass + ")", ms(t0),
                         "-> " + natOut.findings().size() + " correction(s)");
            }
            if (needsRetry(natStrategy, natOut.findings().size(), natPrevCount, text))
                log.warn(String.format("NaturalityCorrector: seuil encore depasse apres relance — %.3f corr/mot (seuil %.3f) ou >= %d corrections",
                                       ratio(natOut.findings().size(), text), correctorConfig.repeatThresholdPerWord(), correctorConfig.minCorrectionsForRetry()));
        }

        // Style
        RetryStrategy styleStrategy = styleCorrectorStep.retryStrategy();
        int stylePrevCount = Integer.MAX_VALUE;
        t0 = System.nanoTime();
        StyleCorrectorOutput styleOut = styleCorrectorStep.run(text, scenario);
        text = applyStyleCorrections(text, styleOut.findings());
        log.step("SequenceStyleCorrector", ms(t0),
                 "-> " + styleOut.findings().size() + " correction(s)");
        if (repeat) {
            for (int pass = 2; pass <= 1 + maxPasses && needsRetry(styleStrategy, styleOut.findings().size(), stylePrevCount, text); pass++) {
                stylePrevCount = styleOut.findings().size();
                t0 = System.nanoTime();
                styleOut = styleCorrectorStep.run(text, scenario);
                text = applyStyleCorrections(text, styleOut.findings());
                log.step("SequenceStyleCorrector (pass " + pass + ")", ms(t0),
                         "-> " + styleOut.findings().size() + " correction(s)");
            }
            if (needsRetry(styleStrategy, styleOut.findings().size(), stylePrevCount, text))
                log.warn(String.format("StyleCorrector: seuil encore depasse apres relance — %.3f corr/mot (seuil %.3f) ou >= %d corrections",
                                       ratio(styleOut.findings().size(), text), correctorConfig.repeatThresholdPerWord(), correctorConfig.minCorrectionsForRetry()));
        }

        // Grammar
        RetryStrategy grammarStrategy = grammarCorrectorStep.retryStrategy();
        int grammarPrevCount = Integer.MAX_VALUE;
        t0 = System.nanoTime();
        GrammarCorrectorOutput grammarOut = grammarCorrectorStep.run(text);
        text = applyGrammarCorrections(text, grammarOut);
        log.step("SequenceGrammarCorrector", ms(t0),
                 "-> " + grammarOut.corrections().size() + " correction(s)");
        if (repeat) {
            for (int pass = 2; pass <= 1 + maxPasses && needsRetry(grammarStrategy, grammarOut.corrections().size(), grammarPrevCount, text); pass++) {
                grammarPrevCount = grammarOut.corrections().size();
                t0 = System.nanoTime();
                grammarOut = grammarCorrectorStep.run(text);
                text = applyGrammarCorrections(text, grammarOut);
                log.step("SequenceGrammarCorrector (pass " + pass + ")", ms(t0),
                         "-> " + grammarOut.corrections().size() + " correction(s)");
            }
            if (needsRetry(grammarStrategy, grammarOut.corrections().size(), grammarPrevCount, text))
                log.warn(String.format("GrammarCorrector: seuil encore depasse apres relance — %.3f corr/mot (seuil %.3f) ou >= %d corrections",
                                       ratio(grammarOut.corrections().size(), text), correctorConfig.repeatThresholdPerWord(), correctorConfig.minCorrectionsForRetry()));
        }

        return text;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3 — Global critique with vote
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs all sequence-level critics together and computes a global average score.
     * All LLM calls complete first; scores are logged together afterwards (mirrors Plan pattern).
     * If score < SEQUENCE_CRITIC_THRESHOLD and retry budget allows:
     *   Writer (all problems as feedback) → Correctors (full Phase 2) → Critics again.
     * Returns the version with the highest average score across all passes.
     */
    private String runSequenceCritique(String text, Scenario scenario, Chapter chapter,
                                        Sequence seq, Story story, String sequencePlan,
                                        GenerationConfig config) {
        int    maxRetry    = config.qualityLevel().sequenceMaxRetry();
        String bestText    = text;
        double bestScore   = -1.0;
        int    bestPass    = 0;
        int    finalPasses = 0;

        for (int pass = 0; pass <= maxRetry; pass++) {
            finalPasses = pass + 1;
            List<CriticEntry> entries     = new ArrayList<>();
            List<String>      allProblems = new ArrayList<>();

            // Run critics — collect results before logging
            long t0 = System.nanoTime();
            DeusInMachinaCriticOutput dm = deusInMachinaCriticStep.run(text, scenario, chapter, seq, sequencePlan);
            entries.add(new CriticEntry("SequenceDeusInMachinaCritic", dm.score(), ms(t0),
                                        dm.hasLeaks() ? List.of(dm.summary()) : List.of()));
            if (dm.hasLeaks()) allProblems.add("Fuites mécaniques : " + dm.summary());

            if (sequencePlan != null && !sequencePlan.isBlank()) {
                List<String> beats = PlanFidelityCriticStep.extractBeats(sequencePlan);
                if (!beats.isEmpty()) {
                    t0 = System.nanoTime();
                    PlanFidelityCriticOutput pf = planFidelityCriticStep.run(text, beats);
                    entries.add(new CriticEntry("SequencePlanFidelityCritic", pf.score(), ms(t0), pf.failures()));
                    pf.failures().forEach(f -> allProblems.add("Beat du plan non développé : " + f));
                }
            }

            List<String> checks = ScenarioFormatters.writerChecks(scenario, chapter, seq);
            if (!checks.isEmpty()) {
                t0 = System.nanoTime();
                CheckCriticOutput cc = checkCriticStep.run(text, checks);
                entries.add(new CriticEntry("SequenceCheckCritic", cc.score(), ms(t0), cc.failures()));
                // TODO: CheckCritic failures not fed to Writer — add dual representation to enable Writer feedback
            }

            // Log all scores together, then decision
            List<Double> scores      = logCritics(entries);
            double       elimination = config.qualityLevel().eliminationThreshold();
            double       minScore    = scores.stream().mapToDouble(Double::doubleValue).min().orElse(10.0);
            boolean      eliminated  = minScore < elimination;
            double  avg      = scores.stream().mapToDouble(Double::doubleValue).average().orElse(10.0);
            boolean avgPassed = avg >= SEQUENCE_CRITIC_THRESHOLD;
            boolean passed   = avgPassed && !eliminated;
            boolean isLast   = pass == maxRetry;

            if (avgPassed && eliminated)
                log.warn(String.format("Sequence : note eliminatoire franchie (seuil %.1f) — relance forcee malgre une moyenne suffisante", elimination));

            log.scoresSummary(avg, SEQUENCE_CRITIC_THRESHOLD, minScore, elimination,
                               passed, (!passed && !isLast) ? "SEQUENCE " + (pass + 2) + "/" + (maxRetry + 1) : null);

            if (avg > bestScore) { bestScore = avg; bestText = text; bestPass = pass + 1; }
            if (passed || isLast) break;

            String feedback = allProblems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
            t0   = System.nanoTime();
            text = writerStep.run(scenario, chapter, seq, story, sequencePlan, feedback).text();
            log.step("SequenceWriter (CritiqueRetry " + (pass + 1) + ")", ms(t0), "-> " + countWords(text) + " mots");
            text = applyCorrectorsPhase(text, scenario, chapter, seq, sequencePlan, config);
        }

        if (finalPasses > 1) log.sequenceRetained(bestPass, finalPasses, bestScore);
        return bestText;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chapter-level critique (after all sequences are written)
    // ─────────────────────────────────────────────────────────────────────────

    private record ChapterCritiqueResult(double avg, String problems, List<Double> scores) {}

    /** Buffers one critic result so all LLM calls can complete before any score is logged. */
    private record CriticEntry(String name, double score, long ms, List<String> problems) {}

    /** Logs all buffered critic scores together and returns the list of scores. */
    private List<Double> logCritics(List<CriticEntry> entries) {
        List<Double> scores = new ArrayList<>();
        for (CriticEntry e : entries) {
            log.critic(e.name(), e.score(), e.ms(), e.problems());
            scores.add(e.score());
        }
        return scores;
    }

    private ChapterCritiqueResult runChapterCritique(Chapter chapter, Scenario scenario,
                                                      String fullText, Story story,
                                                      GenerationConfig config) {
        List<CriticEntry> entries = new ArrayList<>();

        // Run critics — collect results before logging
        switch (chapter.type()) {
            case IMPERATIVE -> {
                long t0 = System.nanoTime();
                ChapterNarrativeCriticOutput tn = textNarrativeCriticStep.run(fullText, chapter, scenario);
                entries.add(new CriticEntry("ChapterNarrativeCritic", tn.score(), ms(t0), tn.problems()));

                t0 = System.nanoTime();
                ChapterCoherenceCriticOutput tc = textCoherenceCriticStep.run(fullText, scenario, chapter);
                entries.add(new CriticEntry("ChapterCoherenceCritic", tc.score(), ms(t0), tc.problems()));
            }
            case DREAM -> {
                long t0 = System.nanoTime();
                ChapterDreamCriticOutput td = textDreamCriticStep.run(fullText, scenario);
                entries.add(new CriticEntry("ChapterDreamCritic", td.score(), ms(t0), td.problems()));
            }
            case WHAT_IF -> {
                long t0 = System.nanoTime();
                ChapterWhatIfCriticOutput tw = textWhatIfCriticStep.run(fullText, scenario, chapter);
                entries.add(new CriticEntry("ChapterWhatIfCritic", tw.score(), ms(t0), tw.problems()));

                t0 = System.nanoTime();
                ChapterCoherenceCriticOutput tc = textCoherenceCriticStep.run(fullText, scenario, chapter);
                entries.add(new CriticEntry("ChapterCoherenceCritic", tc.score(), ms(t0), tc.problems()));
            }
        }

        long t0 = System.nanoTime();
        ChapterGoalCriticOutput gt = goalTextCriticStep.run(fullText, chapter, scenario);
        entries.add(new CriticEntry("ChapterGoalCritic", gt.score(), ms(t0), gt.problems()));

        // Log all scores together
        List<Double> scores   = logCritics(entries);
        List<String> problems = entries.stream().flatMap(e -> e.problems().stream()).toList();

        double avg          = scores.stream().mapToDouble(Double::doubleValue).average().orElse(10.0);
        String problemsText = problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        return new ChapterCritiqueResult(avg, problemsText, scores);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Patch utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String applyDeusCorrections(String text, List<DeusInMachinaCorrectorFinding> findings) {
        return applyCorrections(text, findings, "DeusInMachinaCorrector",
                DeusInMachinaCorrectorFinding::wrongPhrase, DeusInMachinaCorrectorFinding::correctedPhrase);
    }

    private String applyStyleCorrections(String text, List<StyleCorrectorFinding> findings) {
        return applyCorrections(text, findings, "StyleCorrector",
                StyleCorrectorFinding::wrongPhrase, StyleCorrectorFinding::correctedPhrase);
    }

    private String applyNaturalityFixes(String text, List<NaturalityFinding> findings) {
        return applyCorrections(text, findings, "NaturalityCorrector",
                NaturalityFinding::wrongPhrase, NaturalityFinding::correctedPhrase);
    }

    private String applyGrammarCorrections(String text, GrammarCorrectorOutput grammar) {
        return applyCorrections(text, grammar.corrections(), "GrammarCorrector",
                GrammarCorrectorOutput.Correction::wrongSentence, GrammarCorrectorOutput.Correction::correctSentence);
    }

    private String applyPhrasingCorrections(String text, PhrasingCorrectorOutput phrasing) {
        return applyCorrections(text, phrasing.corrections(), "PhrasingCorrector",
                PhrasingCorrectorOutput.Correction::wrongSentence, PhrasingCorrectorOutput.Correction::correctSentence);
    }

    /** Mutualized (wrong → correct) patch loop, shared by every Corrector's apply method. */
    private <T> String applyCorrections(String text, List<T> findings, String correctorLabel,
                                         Function<T, String> wrongOf, Function<T, String> correctOf) {
        String result = text;
        for (T f : findings) {
            String wrong = wrongOf.apply(f);
            TextPatcher.Result patched = TextPatcher.apply(result, wrong, correctOf.apply(f));
            if (!patched.applied())
                log.warn(correctorLabel + ": replace miss — phrase not found in text: " + wrong);
            result = patched.text();
        }
        return result;
    }

    private float ratio(int count, String text) {
        int words = countWords(text);
        return words == 0 ? 0f : (float) count / words;
    }

    /**
     * Dispatches the retry decision per the corrector's own {@link RetryStrategy}.
     * previousCount is the finding count of the pass before this one (Integer.MAX_VALUE before
     * any repeat pass has run yet, so DECREASING/DECREASING_AND_RATIO_THRESHOLD never block the
     * first repeat attempt on a trend they don't have data for yet).
     */
    private boolean needsRetry(RetryStrategy strategy, int count, int previousCount, String text) {
        boolean ratioCond      = ratio(count, text) > correctorConfig.repeatThresholdPerWord()
                                  || count >= correctorConfig.minCorrectionsForRetry();
        boolean decreasingCond = count < previousCount;
        return switch (strategy) {
            case SINGLE_PASS                   -> false;
            case RATIO_THRESHOLD               -> ratioCond;
            case DECREASING                    -> decreasingCond;
            case DECREASING_AND_RATIO_THRESHOLD -> ratioCond && decreasingCond;
        };
    }

    private static int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.split("\\s+").length;
    }

    private static int countLines(String text) {
        if (text == null || text.isBlank()) return 0;
        return (int) text.lines().filter(l -> !l.isBlank()).count();
    }

    private static long ms(long nanoStart) {
        return (System.nanoTime() - nanoStart) / 1_000_000L;
    }
}
