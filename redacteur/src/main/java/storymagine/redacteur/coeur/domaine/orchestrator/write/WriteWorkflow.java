package storymagine.redacteur.coeur.domaine.orchestrator.write;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker.DeusInMachinaCheckerOutput;
import storymagine.redacteur.coeur.ports.HtmlExportPort;
import storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker.GoalTextCheckerOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.proofreader.ProofreaderOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilterOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTrackerOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker.SequenceCheckerOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker.SequenceStyleCheckerOutput;
import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractorOutput;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the writing phase for one chapter.
 *
 * Per sequence:
 *   Writer → Proofreader
 *   → DeusInMachinaChecker (retry loop, up to sequenceMaxRetry)
 *   → SequenceStyleChecker  (retry loop, up to sequenceMaxRetry, if score < STYLE_THRESHOLD)
 *   → SequenceChecker       (retry loop, up to sequenceMaxRetry, if failures present)
 *   → StateExtractor → RepetitionTracker → RepetitionFilter
 *
 * After all sequences (STANDARD / FULL modes):
 *   Chapter critics on full text → retry all sequences if avg ≤ chapitreThreshold.
 *   Best chapter version (highest average critic score) is retained.
 *
 * DREAM / WHAT_IF chapters use snapshot/restore on WorldState.
 */
public class WriteWorkflow {

    private static final int STYLE_THRESHOLD = 7;

    private final WriterStep               writerStep;
    private final ProofreaderStep          proofreaderStep;
    private final StateExtractorStep       stateExtractorStep;
    private final RepetitionTrackerStep    repetitionTrackerStep;
    private final RepetitionFilterStep     repetitionFilterStep;
    private final SequenceCheckerStep      sequenceCheckerStep;
    private final SequenceStyleCheckerStep sequenceStyleCheckerStep;
    private final TextNarrativeCriticStep  textNarrativeCriticStep;
    private final TextCoherenceCriticStep  textCoherenceCriticStep;
    private final TextDreamCriticStep      textDreamCriticStep;
    private final TextWhatIfCriticStep     textWhatIfCriticStep;
    private final DeusInMachinaCheckerStep deusInMachinaCheckerStep;
    private final GoalTextCheckerStep      goalTextCheckerStep;
    private final HtmlExportPort           htmlExport;
    private final LogPort                  log;

    public WriteWorkflow(WriterStep writerStep,
                         ProofreaderStep proofreaderStep,
                         StateExtractorStep stateExtractorStep,
                         RepetitionTrackerStep repetitionTrackerStep,
                         RepetitionFilterStep repetitionFilterStep,
                         SequenceCheckerStep sequenceCheckerStep,
                         SequenceStyleCheckerStep sequenceStyleCheckerStep,
                         TextNarrativeCriticStep textNarrativeCriticStep,
                         TextCoherenceCriticStep textCoherenceCriticStep,
                         TextDreamCriticStep textDreamCriticStep,
                         TextWhatIfCriticStep textWhatIfCriticStep,
                         DeusInMachinaCheckerStep deusInMachinaCheckerStep,
                         GoalTextCheckerStep goalTextCheckerStep,
                         HtmlExportPort htmlExport,
                         LogPort log) {
        this.writerStep               = writerStep;
        this.proofreaderStep          = proofreaderStep;
        this.stateExtractorStep       = stateExtractorStep;
        this.repetitionTrackerStep    = repetitionTrackerStep;
        this.repetitionFilterStep     = repetitionFilterStep;
        this.sequenceCheckerStep      = sequenceCheckerStep;
        this.sequenceStyleCheckerStep = sequenceStyleCheckerStep;
        this.textNarrativeCriticStep  = textNarrativeCriticStep;
        this.textCoherenceCriticStep  = textCoherenceCriticStep;
        this.textDreamCriticStep      = textDreamCriticStep;
        this.textWhatIfCriticStep     = textWhatIfCriticStep;
        this.deusInMachinaCheckerStep = deusInMachinaCheckerStep;
        this.goalTextCheckerStep      = goalTextCheckerStep;
        this.htmlExport               = htmlExport;
        this.log                      = log;
    }

    /** Writes all sequences of the current chapter in Story. Mutates Story. */
    public void run(Scenario scenario, Chapter chapter, Story story, GenerationConfig config) {
        boolean isolated = chapter.type() != NarrativeType.IMPERATIVE;
        WorldState.Snapshot snapshot = isolated ? story.worldState().snapshot() : null;

        List<Sequence> sequences       = chapter.sequences();
        boolean        runCritique     = config.qualityLevel().runsSequenceCheckers();
        int            chapMaxAttempts = runCritique ? 1 + config.qualityLevel().chapitreMaxRetry() : 1;

        List<WrittenSequence> bestSequences = null;
        double                bestScore     = -1.0;
        String                chapterProblems = null;  // problems fed back to writer on retry

        for (int chapPass = 0; chapPass < chapMaxAttempts; chapPass++) {

            WrittenChapter wc = story.currentChapter().orElseThrow();
            if (chapPass > 0) {
                wc.clearSequences();
            }

            for (int i = 0; i < sequences.size(); i++) {
                writeSequence(scenario, chapter, sequences.get(i), story, config,
                              i + 1, sequences.size(), chapterProblems);
            }

            if (!runCritique) break;

            // ── Chapter-level critique on full text ───────────────────────────
            ChapterCritiqueResult critique = runChapterCritique(chapter, scenario,
                                                                wc.fullText(), story, config);
            boolean passed        = critique.avg() > config.qualityLevel().chapitreThreshold();
            boolean isLastAttempt = chapPass == chapMaxAttempts - 1;

            if (bestSequences == null || critique.avg() > bestScore) {
                bestScore     = critique.avg();
                bestSequences = new ArrayList<>(wc.sequences());
            }

            String hint = (!passed && !isLastAttempt) ? (chapPass + 2) + "/" + chapMaxAttempts : null;
            log.scoresSummary(critique.avg(), passed, hint);

            if (passed || isLastAttempt) break;

            chapterProblems = critique.problems();
        }

        // ── Restore the best chapter version if retries occurred ─────────────
        if (runCritique && bestSequences != null) {
            WrittenChapter wc = story.currentChapter().orElseThrow();
            wc.clearSequences();
            for (WrittenSequence seq : bestSequences) {
                wc.addSequence(seq);
            }
        }

        if (isolated) {
            story.worldState().restore(snapshot);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-sequence writing
    // ─────────────────────────────────────────────────────────────────────────

    private void writeSequence(Scenario scenario, Chapter chapter, Sequence seq,
                                Story story, GenerationConfig config,
                                int seqIdx, int seqTotal, String chapterRewriteProblems) {
        log.phaseHeader("WRITE " + seqIdx + "/" + seqTotal, seq.type().name().toLowerCase());

        String actionsText = "";
        long   t0;

        List<String> seqPlans    = story.currentChapter().orElseThrow().sequencePlans();
        String       sequencePlan = (!seqPlans.isEmpty() && seqIdx <= seqPlans.size())
            ? seqPlans.get(seqIdx - 1) : null;

        // Initial write — pass chapter-level rewrite problems if this is a retry pass
        t0 = System.nanoTime();
        String text = writerStep.run(scenario, chapter, seq, story, sequencePlan,
                                     actionsText, chapterRewriteProblems).text();
        log.step("WriterStep", ms(t0), "-> " + countWords(text) + " mots");

        // ── Proofreader ───────────────────────────────────────────────────────
        if (config.qualityLevel().runsProofreader()) {
            t0 = System.nanoTime();
            ProofreaderOutput proofOut = proofreaderStep.run(text);
            text = applyCorrections(text, proofOut);
            String proofNote = proofOut.corrections().isEmpty() ? null
                    : "-> " + proofOut.corrections().size() + " correction(s)";
            log.step("ProofreaderStep", ms(t0), proofNote);
        }

        if (config.qualityLevel().runsSequenceCheckers()) {
            text = runSequenceCheckers(scenario, chapter, seq, story, sequencePlan,
                                       actionsText, text, seqIdx, seqTotal, config);
        }

        // ── Memory updates (after all per-sequence rewrites) ──────────────────
        t0 = System.nanoTime();
        StateExtractorOutput stateOut = stateExtractorStep.run(text, story.worldState());
        if (stateOut.hasChanges()) {
            StoryFormatters.applyStateLines(story.worldState(), stateOut.stateLines());
        }
        String stateNote = stateOut.hasChanges()
                ? "-> " + countLines(stateOut.stateLines()) + " changement(s)" : null;
        log.step("StateExtractorStep", ms(t0), stateNote);

        t0 = System.nanoTime();
        RepetitionTrackerOutput trackOut = repetitionTrackerStep.run(text, story.repetitionMemory());
        log.step("RepetitionTrackerStep", ms(t0), null);

        t0 = System.nanoTime();
        RepetitionFilterOutput filterOut = repetitionFilterStep.run(
                trackOut.phrases(),
                ScenarioFormatters.keepPhrases(scenario));
        story.repetitionMemory().addPhrases(filterOut.filteredCandidates());
        story.repetitionMemory().addThemes(trackOut.themes());
        String filterNote = filterOut.filteredCandidates().isEmpty() ? null
                : "-> " + filterOut.filteredCandidates().size() + " filtre(s)";
        log.step("RepetitionFilterStep", ms(t0), filterNote);

        story.currentChapter().orElseThrow().addSequence(new WrittenSequence(sequencePlan, text));
        log.sequenceText(chapter.title(), seqIdx, text);
        htmlExport.exportHtml(scenario.config().title(), story);
    }

    /**
     * Per-sequence checks with independent retry loops (Redacteur logic):
     * 1. DeusInMachina   — retry if mechanical leaks detected
     * 2. SequenceStyleChecker — retry if score < STYLE_THRESHOLD
     * 3. SequenceChecker — retry if required elements missing
     * Each loop is capped at qualityLevel.sequenceMaxRetry().
     */
    private String runSequenceCheckers(Scenario scenario, Chapter chapter, Sequence seq,
                                        Story story, String sequencePlan, String actionsText,
                                        String text, int seqIdx, int seqTotal,
                                        GenerationConfig config) {

        int maxRetry = config.qualityLevel().sequenceMaxRetry();

        // 1. DeusInMachina
        for (int r = 0; r < 1 + maxRetry; r++) {
            long                    t0 = System.nanoTime();
            DeusInMachinaCheckerOutput dm = deusInMachinaCheckerStep.run(text, scenario, chapter);
            boolean                 pass = !dm.hasLeaks();
            log.critic("DeusInMachinaChecker", pass ? 10.0 : 4.0, ms(t0),
                       pass ? List.of() : List.of(dm.summary()));
            if (pass || r == maxRetry) break;
            String problems = "Fuites mécaniques à éliminer :\n"
                    + dm.leaks().stream().map(l -> "- " + l).collect(Collectors.joining("\n"));
            t0   = System.nanoTime();
            text = writerStep.run(scenario, chapter, seq, story, sequencePlan,
                                  actionsText, problems).text();
            log.step("WriterStep (DeusRetry " + (r + 1) + ")", ms(t0),
                     "-> " + countWords(text) + " mots");
        }

        // 2. SequenceStyleChecker
        for (int r = 0; r < 1 + maxRetry; r++) {
            long                        t0  = System.nanoTime();
            SequenceStyleCheckerOutput  ss  = sequenceStyleCheckerStep.run(text, scenario);
            boolean                     pass = ss.passed(STYLE_THRESHOLD);
            log.critic("SequenceStyleChecker", ss.score(), ms(t0),
                       pass ? List.of() : ss.problems());
            if (pass || r == maxRetry) break;
            String problems = "Corrections stylistiques :\n"
                    + ss.problems().stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
            t0   = System.nanoTime();
            text = writerStep.run(scenario, chapter, seq, story, sequencePlan,
                                  actionsText, problems).text();
            log.step("WriterStep (StyleRetry " + (r + 1) + ")", ms(t0),
                     "-> " + countWords(text) + " mots");
        }

        // 3. SequenceChecker (only if any writer checks declared at any level)
        if (!ScenarioFormatters.writerChecks(scenario, chapter, seq).isEmpty()) {
            for (int r = 0; r < 1 + maxRetry; r++) {
                long                  t0   = System.nanoTime();
                SequenceCheckerOutput sc   = sequenceCheckerStep.run(seq, text, scenario, chapter);
                boolean               pass = sc.failures().isEmpty();
                log.critic("SequenceChecker", sc.score(), ms(t0),
                           pass ? List.of() : sc.failures());
                if (pass || r == maxRetry) break;
                String problems = "Éléments requis manquants :\n"
                        + sc.failures().stream().map(f -> "- " + f).collect(Collectors.joining("\n"));
                t0   = System.nanoTime();
                text = writerStep.run(scenario, chapter, seq, story, sequencePlan,
                                      actionsText, problems).text();
                log.step("WriterStep (SeqCheckRetry " + (r + 1) + ")", ms(t0),
                         "-> " + countWords(text) + " mots");
            }
        }

        return text;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Chapter-level critique (after all sequences are written)
    // ─────────────────────────────────────────────────────────────────────────

    private record ChapterCritiqueResult(double avg, String problems) {}

    private ChapterCritiqueResult runChapterCritique(Chapter chapter, Scenario scenario,
                                                      String fullText, Story story,
                                                      GenerationConfig config) {
        List<Double>  scores   = new ArrayList<>();
        List<String>  problems = new ArrayList<>();

        switch (chapter.type()) {
            case IMPERATIVE -> {
                long t0 = System.nanoTime();
                TextNarrativeCriticOutput tn = textNarrativeCriticStep.run(fullText, scenario);
                log.critic("TextNarrativeCritic", tn.score(), ms(t0), tn.problems());
                scores.add(tn.score());
                problems.addAll(tn.problems());

                t0 = System.nanoTime();
                TextCoherenceCriticOutput tc = textCoherenceCriticStep.run(fullText, scenario, chapter);
                log.critic("TextCoherenceCritic", tc.score(), ms(t0), tc.problems());
                scores.add(tc.score());
                problems.addAll(tc.problems());
            }
            case DREAM -> {
                long t0 = System.nanoTime();
                TextDreamCriticOutput td = textDreamCriticStep.run(fullText, scenario);
                log.critic("TextDreamCritic", td.score(), ms(t0), td.problems());
                scores.add(td.score());
                problems.addAll(td.problems());
            }
            case WHAT_IF -> {
                long t0 = System.nanoTime();
                TextWhatIfCriticOutput tw = textWhatIfCriticStep.run(fullText, scenario, chapter);
                log.critic("TextWhatIfCritic", tw.score(), ms(t0), tw.problems());
                scores.add(tw.score());
                problems.addAll(tw.problems());

                t0 = System.nanoTime();
                TextCoherenceCriticOutput tc = textCoherenceCriticStep.run(fullText, scenario, chapter);
                log.critic("TextCoherenceCritic", tc.score(), ms(t0), tc.problems());
                scores.add(tc.score());
                problems.addAll(tc.problems());
            }
        }

        long                t0 = System.nanoTime();
        GoalTextCheckerOutput gt = goalTextCheckerStep.run(fullText, chapter, scenario);
        log.critic("GoalTextChecker", gt.score(), ms(t0), gt.problems());
        scores.add(gt.score());
        problems.addAll(gt.problems());

        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(10.0);
        String problemsText = problems.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));
        return new ChapterCritiqueResult(avg, problemsText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private static String applyCorrections(String text, ProofreaderOutput proof) {
        if (proof.corrections().isEmpty()) return text;
        String result = text;
        for (ProofreaderOutput.Correction c : proof.corrections()) {
            result = result.replace(c.wrongSentence(), c.correctSentence());
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
