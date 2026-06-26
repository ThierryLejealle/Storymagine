package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacorrector.DeusInMachinaCorrectorFinding;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacorrector.DeusInMachinaCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacritic.DeusInMachinaCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.checkcritic.CheckCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.planfidelitycritic.PlanFidelityCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.goaltextcritic.GoalTextCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector.NaturalityFinding;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector.NaturalityCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.proofreadercorrector.ProofreaderCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilterOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTrackerOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractorOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector.StyleCorrectorFinding;
import storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector.StyleCorrectorOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.textcoherencecritic.TextCoherenceCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.textdreamcritic.TextDreamCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.textnarrativecritic.TextNarrativeCriticOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.textwhatifcritic.TextWhatIfCriticOutput;
import storymagine.redacteur.coeur.domaine.orchestrator.GenerationConfig;
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
import java.util.stream.Collectors;

/**
 * Orchestrates the writing phase for one chapter.
 *
 * Per sequence:
 *   PHASE 1 — WRITE
 *     Writer
 *
 *   PHASE 2 — CORRECTORS  (skip if BROUILLON)
 *     DeusInMachinaCorrector → NaturalityCorrector → ProofreaderCorrector
 *     Patches applied inline via String.replace — no Writer retry.
 *
 *   PHASE 3 — GLOBAL CRITIQUE  (skip if BROUILLON)
 *     All critics run together: DeusInMachinaCritic, StyleCritic, PlanFidelityCritic (if beats), CheckCritic (if checks).
 *     Global average score computed from all critics.
 *     If score < SEQUENCE_CRITIC_THRESHOLD AND retry budget remains:
 *       → Writer (ALL collected problems as feedback) → Phase 2 again → Critics again
 *     Best version (highest average score) is retained across passes.
 *
 *   PHASE 4 — POST-PROCESSING  (always)
 *     StateExtractor → RepetitionTracker → RepetitionFilter
 *
 * Chapter level (after all sequences, STANDARD / FULL modes):
 *   Chapter critics run together on full text → global average.
 *   If avg < chapitreThreshold AND retry budget remains: REBOOT all sequences with feedback.
 *   Best chapter version (highest average) is retained.
 *
 * DREAM / WHAT_IF chapters use snapshot/restore on WorldState.
 */
public class WriteWorkflow {

    private static final int SEQUENCE_CRITIC_THRESHOLD = 7;

    // Phase 1
    private final WriterStep                 writerStep;

    // Phase 2 — Correctors (ordered)
    private final DeusInMachinaCorrectorStep deusInMachinaCorrectorStep;
    private final NaturalityCorrectorStep    naturalityCorrectorStep;
    private final ProofreaderCorrectorStep   proofreaderCorrectorStep;

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
    private final TextNarrativeCriticStep    textNarrativeCriticStep;
    private final TextCoherenceCriticStep    textCoherenceCriticStep;
    private final TextDreamCriticStep        textDreamCriticStep;
    private final TextWhatIfCriticStep       textWhatIfCriticStep;
    private final GoalTextCriticStep         goalTextCriticStep;

    private final HtmlExportPort             htmlExport;
    private final LogPort                    log;

    public WriteWorkflow(WriterStep writerStep,
                         DeusInMachinaCorrectorStep deusInMachinaCorrectorStep,
                         NaturalityCorrectorStep naturalityCorrectorStep,
                         ProofreaderCorrectorStep proofreaderCorrectorStep,
                         StyleCorrectorStep styleCorrectorStep,
                         DeusInMachinaCriticStep deusInMachinaCriticStep,
                         PlanFidelityCriticStep planFidelityCriticStep,
                         CheckCriticStep checkCriticStep,
                         StateExtractorStep stateExtractorStep,
                         RepetitionTrackerStep repetitionTrackerStep,
                         RepetitionFilterStep repetitionFilterStep,
                         TextNarrativeCriticStep textNarrativeCriticStep,
                         TextCoherenceCriticStep textCoherenceCriticStep,
                         TextDreamCriticStep textDreamCriticStep,
                         TextWhatIfCriticStep textWhatIfCriticStep,
                         GoalTextCriticStep goalTextCriticStep,
                         HtmlExportPort htmlExport,
                         LogPort log) {
        this.writerStep                 = writerStep;
        this.deusInMachinaCorrectorStep = deusInMachinaCorrectorStep;
        this.naturalityCorrectorStep    = naturalityCorrectorStep;
        this.proofreaderCorrectorStep   = proofreaderCorrectorStep;
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

            ChapterCritiqueResult critique = runChapterCritique(chapter, scenario,
                                                                wc.fullText(), story, config);
            boolean passed        = critique.avg() > config.qualityLevel().chapitreThreshold();
            boolean isLastAttempt = chapPass == chapMaxAttempts - 1;

            if (bestSequences == null || critique.avg() > bestScore) {
                bestScore     = critique.avg();
                bestSequences = new ArrayList<>(wc.sequences());
                bestChapPass  = chapPass + 1;
            }

            String hint = (!passed && !isLastAttempt) ? (chapPass + 2) + "/" + chapMaxAttempts : null;
            log.scoresSummary(critique.avg(), passed, hint);

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
                                     "", chapterRewriteProblems).text();
        log.step("Writer", ms(t0), "-> " + countWords(text) + " mots");

        // Phase 2 — Correctors
        text = applyCorrectorsPhase(text, scenario, chapter, config);

        // Phase 3 — Global critique with vote
        if (config.qualityLevel().runsSequenceCheckers()) {
            text = runSequenceCritique(text, scenario, chapter, seq, story, sequencePlan, config);
        }

        // Phase 4 — Post-processing
        t0 = System.nanoTime();
        StateExtractorOutput stateOut = stateExtractorStep.run(text, story.worldState());
        if (stateOut.hasChanges()) StoryFormatters.applyStateLines(story.worldState(), stateOut.stateLines());
        log.step("StateExtractor", ms(t0),
                 stateOut.hasChanges() ? "-> " + countLines(stateOut.stateLines()) + " changement(s)" : null);

        t0 = System.nanoTime();
        RepetitionTrackerOutput trackOut = repetitionTrackerStep.run(text, story.repetitionMemory());
        log.step("RepetitionTracker", ms(t0), null);

        t0 = System.nanoTime();
        RepetitionFilterOutput filterOut = repetitionFilterStep.run(
                trackOut.phrases(), ScenarioFormatters.keepPhrases(scenario));
        story.repetitionMemory().addPhrases(filterOut.filteredCandidates());
        story.repetitionMemory().addThemes(trackOut.themes());
        log.step("RepetitionFilter", ms(t0),
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
                                         GenerationConfig config) {
        if (!config.qualityLevel().runsProofreader()) return text;

        long t0 = System.nanoTime();
        DeusInMachinaCorrectorOutput deusOut = deusInMachinaCorrectorStep.run(text, scenario, chapter);
        text = applyDeusCorrections(text, deusOut.findings());
        log.step("DeusInMachinaCorrector", ms(t0),
                 deusOut.findings().isEmpty() ? null : "-> " + deusOut.findings().size() + " correction(s)");

        t0 = System.nanoTime();
        NaturalityCorrectorOutput natOut = naturalityCorrectorStep.run(text);
        text = applyNaturalityFixes(text, natOut.findings());
        log.step("NaturalityCorrector", ms(t0),
                 natOut.findings().isEmpty() ? null : "-> " + natOut.findings().size() + " correction(s)");

        t0 = System.nanoTime();
        StyleCorrectorOutput styleOut = styleCorrectorStep.run(text, scenario);
        text = applyStyleCorrections(text, styleOut.findings());
        log.step("StyleCorrector", ms(t0),
                 styleOut.findings().isEmpty() ? null : "-> " + styleOut.findings().size() + " correction(s)");

        t0 = System.nanoTime();
        ProofreaderCorrectorOutput proofOut = proofreaderCorrectorStep.run(text);
        text = applyProofCorrections(text, proofOut);
        log.step("ProofreaderCorrector", ms(t0),
                 proofOut.corrections().isEmpty() ? null : "-> " + proofOut.corrections().size() + " correction(s)");

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
            DeusInMachinaCriticOutput dm = deusInMachinaCriticStep.run(text, scenario, chapter);
            entries.add(new CriticEntry("DeusInMachinaCritic", dm.score(), ms(t0),
                                        dm.hasLeaks() ? List.of(dm.summary()) : List.of()));
            if (dm.hasLeaks()) allProblems.add("Fuites mécaniques : " + dm.summary());

            if (sequencePlan != null && !sequencePlan.isBlank()) {
                List<String> beats = PlanFidelityCriticStep.extractBeats(sequencePlan);
                if (!beats.isEmpty()) {
                    t0 = System.nanoTime();
                    PlanFidelityCriticOutput pf = planFidelityCriticStep.run(text, beats);
                    entries.add(new CriticEntry("PlanFidelityCritic", pf.score(), ms(t0), pf.failures()));
                    pf.failures().forEach(f -> allProblems.add("Beat du plan non développé : " + f));
                }
            }

            List<String> checks = ScenarioFormatters.writerChecks(scenario, chapter, seq);
            if (!checks.isEmpty()) {
                t0 = System.nanoTime();
                CheckCriticOutput cc = checkCriticStep.run(text, checks);
                entries.add(new CriticEntry("CheckCritic", cc.score(), ms(t0), cc.failures()));
                // TODO: CheckCritic failures not fed to Writer — add dual representation to enable Writer feedback
            }

            // Log all scores together, then decision
            List<Double> scores = logCritics(entries);
            double  avg    = scores.stream().mapToDouble(Double::doubleValue).average().orElse(10.0);
            boolean passed = avg >= SEQUENCE_CRITIC_THRESHOLD;
            boolean isLast = pass == maxRetry;
            log.scoresSummary(avg, passed, (!passed && !isLast) ? (pass + 2) + "/" + (maxRetry + 1) : null);

            if (avg > bestScore) { bestScore = avg; bestText = text; bestPass = pass + 1; }
            if (passed || isLast) break;

            String feedback = allProblems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
            t0   = System.nanoTime();
            text = writerStep.run(scenario, chapter, seq, story, sequencePlan, "", feedback).text();
            log.step("Writer (CritiqueRetry " + (pass + 1) + ")", ms(t0), "-> " + countWords(text) + " mots");
            text = applyCorrectorsPhase(text, scenario, chapter, config);
        }

        if (finalPasses > 1) log.sequenceRetained(bestPass, finalPasses, bestScore);
        return bestText;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chapter-level critique (after all sequences are written)
    // ─────────────────────────────────────────────────────────────────────────

    private record ChapterCritiqueResult(double avg, String problems) {}

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
                TextNarrativeCriticOutput tn = textNarrativeCriticStep.run(fullText, scenario);
                entries.add(new CriticEntry("TextNarrativeCritic", tn.score(), ms(t0), tn.problems()));

                t0 = System.nanoTime();
                TextCoherenceCriticOutput tc = textCoherenceCriticStep.run(fullText, scenario, chapter);
                entries.add(new CriticEntry("TextCoherenceCritic", tc.score(), ms(t0), tc.problems()));
            }
            case DREAM -> {
                long t0 = System.nanoTime();
                TextDreamCriticOutput td = textDreamCriticStep.run(fullText, scenario);
                entries.add(new CriticEntry("TextDreamCritic", td.score(), ms(t0), td.problems()));
            }
            case WHAT_IF -> {
                long t0 = System.nanoTime();
                TextWhatIfCriticOutput tw = textWhatIfCriticStep.run(fullText, scenario, chapter);
                entries.add(new CriticEntry("TextWhatIfCritic", tw.score(), ms(t0), tw.problems()));

                t0 = System.nanoTime();
                TextCoherenceCriticOutput tc = textCoherenceCriticStep.run(fullText, scenario, chapter);
                entries.add(new CriticEntry("TextCoherenceCritic", tc.score(), ms(t0), tc.problems()));
            }
        }

        long t0 = System.nanoTime();
        GoalTextCriticOutput gt = goalTextCriticStep.run(fullText, chapter, scenario);
        entries.add(new CriticEntry("GoalTextCritic", gt.score(), ms(t0), gt.problems()));

        // Log all scores together
        List<Double> scores   = logCritics(entries);
        List<String> problems = entries.stream().flatMap(e -> e.problems().stream()).toList();

        double avg          = scores.stream().mapToDouble(Double::doubleValue).average().orElse(10.0);
        String problemsText = problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        return new ChapterCritiqueResult(avg, problemsText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Patch utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String applyDeusCorrections(String text, List<DeusInMachinaCorrectorFinding> findings) {
        String result = text;
        for (DeusInMachinaCorrectorFinding f : findings) {
            String patched = result.replace(f.wrongPhrase(), f.correctedPhrase());
            if (patched.equals(result))
                log.warn("DeusInMachinaCorrector: replace miss — phrase not found in text: " + f.wrongPhrase());
            result = patched;
        }
        return result;
    }

    private String applyStyleCorrections(String text, List<StyleCorrectorFinding> findings) {
        String result = text;
        for (StyleCorrectorFinding f : findings) {
            String patched = result.replace(f.wrongPhrase(), f.correctedPhrase());
            if (patched.equals(result))
                log.warn("StyleCorrector: replace miss — phrase not found in text: " + f.wrongPhrase());
            result = patched;
        }
        return result;
    }

    private String applyNaturalityFixes(String text, List<NaturalityFinding> findings) {
        String result = text;
        for (NaturalityFinding f : findings) {
            String patched = result.replace(f.wrongPhrase(), f.correctedPhrase());
            if (patched.equals(result))
                log.warn("NaturalityCorrector: replace miss — phrase not found in text: " + f.wrongPhrase());
            result = patched;
        }
        return result;
    }

    private String applyProofCorrections(String text, ProofreaderCorrectorOutput proof) {
        if (proof.corrections().isEmpty()) return text;
        String result = text;
        for (ProofreaderCorrectorOutput.Correction c : proof.corrections()) {
            String patched = result.replace(c.wrongSentence(), c.correctSentence());
            if (patched.equals(result))
                log.warn("ProofreaderCorrector: replace miss — phrase not found in text: " + c.wrongSentence());
            result = patched;
        }
        return result;
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
