package storymagine.redacteur;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaConfig;
import storymagine.redacteur.coeur.domaine.agent.global.causalanalyzer.CausalAnalyzer;
import storymagine.redacteur.coeur.domaine.agent.global.chapterstylechecker.ChapterStyleChecker;
import storymagine.redacteur.coeur.domaine.agent.global.characterchecker.CharacterChecker;
import storymagine.redacteur.coeur.domaine.agent.global.narrativearcanalyzer.NarrativeArcAnalyzer;
import storymagine.redacteur.coeur.domaine.agent.global.storycompressor.StoryCompressor;
import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlanner;
import storymagine.redacteur.coeur.domaine.agent.plan.goalplanchecker.GoalPlanChecker;
import storymagine.redacteur.coeur.domaine.agent.plan.plancoherencecritic.PlanCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.plannarrativecritic.PlanNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinachecker.DeusInMachinaChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.goaltextchecker.GoalTextChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.proofreader.Proofreader;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilter;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTracker;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencechecker.SequenceChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencestylechecker.SequenceStyleChecker;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencewriter.Writer;
import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractor;
import storymagine.redacteur.coeur.domaine.agent.writer.textcoherencecritic.TextCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.textdreamcritic.TextDreamCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.textnarrativecritic.TextNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.textwhatifcritic.TextWhatIfCritic;
import storymagine.redacteur.coeur.domaine.orchestrator.EngineConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.StoryOrchestrator;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.CausalAnalyzerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.ChapterStyleCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.CharacterCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.EvaluateWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.NarrativeArcAnalyzerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.StoryCompressorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.ChapterPlannerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.GoalPlanCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.DeusInMachinaCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.GoalTextCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.ProofreaderStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.RepetitionFilterStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.RepetitionTrackerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.SequenceCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.SequenceStyleCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StateExtractorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.TextCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.TextDreamCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.TextNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.TextWhatIfCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriteWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriterStep;
import storymagine.redacteur.coeur.ports.HtmlExportPort;
import storymagine.redacteur.coeur.ports.ScenarioReaderPort;
import storymagine.redacteur.coeur.service.RedacteurService;
import storymagine.redacteur.coeur.service.RedacteurServiceImpl;
import storymagine.redacteur.infra.EngineConfigLoader;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

import java.nio.file.Path;

/** Wires the redacteur domain with its infrastructure adapters. */
public class RedacteurModule {

    public static RedacteurService assemble(OllamaConfig ollamaConfig, String modelName) {
        LogPort       log    = new ConsoleLogAdapter();
        OllamaAdapter llm    = ollamaConfig.adapter(modelName, log);
        EngineConfig  engine = EngineConfigLoader.load(Path.of("engine.properties"));
        return assemble(llm, new ScenarioFileAdapter(), log, HtmlExportPort.NOOP, engine);
    }

    public static RedacteurService assemble(OllamaConfig ollamaConfig, String modelName,
                                            LogPort log, HtmlExportPort htmlExport) {
        OllamaAdapter llm    = ollamaConfig.adapter(modelName, log);
        EngineConfig  engine = EngineConfigLoader.load(Path.of("engine.properties"));
        return assemble(llm, new ScenarioFileAdapter(), log, htmlExport, engine);
    }

    public static RedacteurService assemble(OllamaAdapter llm, ScenarioReaderPort scenarioReader) {
        EngineConfig engine = EngineConfigLoader.load(Path.of("engine.properties"));
        return assemble(llm, scenarioReader, new ConsoleLogAdapter(), HtmlExportPort.NOOP, engine);
    }

    /** Test-friendly overload — uses EngineConfig.defaults(), no file I/O. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log) {
        return assemble(llm, scenarioReader, log, HtmlExportPort.NOOP, EngineConfig.defaults());
    }

    /** Full assembly — all parameters explicit. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log, HtmlExportPort htmlExport,
                                            EngineConfig engineConfig) {
        // --- Plan agents ---
        var chapterPlanner      = new ChapterPlanner(llm);
        var planNarrativeCritic = new PlanNarrativeCritic(llm);
        var planCoherenceCritic = new PlanCoherenceCritic(llm);
        var goalPlanChecker     = new GoalPlanChecker(llm);

        // --- Writer agents ---
        var writer               = new Writer(llm);
        var deusInMachina        = new DeusInMachinaChecker(llm);
        var goalTextChecker      = new GoalTextChecker(llm);
        var proofreader          = new Proofreader(llm);
        var repetitionFilter     = new RepetitionFilter(llm);
        var repetitionTracker    = new RepetitionTracker(llm);
        var sequenceChecker      = new SequenceChecker(llm);
        var sequenceStyleChecker = new SequenceStyleChecker(llm);
        var stateExtractor       = new StateExtractor(llm);
        var textCoherenceCritic  = new TextCoherenceCritic(llm);
        var textDreamCritic      = new TextDreamCritic(llm);
        var textNarrativeCritic  = new TextNarrativeCritic(llm);
        var textWhatIfCritic     = new TextWhatIfCritic(llm);

        // --- Global agents ---
        var storyCompressor      = new StoryCompressor(llm);
        var chapterStyleChecker  = new ChapterStyleChecker(llm);
        var characterChecker     = new CharacterChecker(llm);
        var narrativeArcAnalyzer = new NarrativeArcAnalyzer(llm);
        var causalAnalyzer       = new CausalAnalyzer(llm);

        // --- Plan steps ---
        var chapterPlannerStep      = new ChapterPlannerStep(chapterPlanner);
        var planNarrativeCriticStep = new PlanNarrativeCriticStep(planNarrativeCritic);
        var planCoherenceCriticStep = new PlanCoherenceCriticStep(planCoherenceCritic);
        var goalPlanCheckerStep     = new GoalPlanCheckerStep(goalPlanChecker);

        // --- Write steps ---
        var writerStep               = new WriterStep(writer);
        var proofreaderStep          = new ProofreaderStep(proofreader);
        var stateExtractorStep       = new StateExtractorStep(stateExtractor);
        var repetitionTrackerStep    = new RepetitionTrackerStep(repetitionTracker);
        var repetitionFilterStep     = new RepetitionFilterStep(repetitionFilter);
        var sequenceCheckerStep      = new SequenceCheckerStep(sequenceChecker);
        var sequenceStyleCheckerStep = new SequenceStyleCheckerStep(sequenceStyleChecker);
        var textNarrativeCriticStep  = new TextNarrativeCriticStep(textNarrativeCritic);
        var textCoherenceCriticStep  = new TextCoherenceCriticStep(textCoherenceCritic);
        var textDreamCriticStep      = new TextDreamCriticStep(textDreamCritic);
        var textWhatIfCriticStep     = new TextWhatIfCriticStep(textWhatIfCritic);
        var deusInMachinaStep        = new DeusInMachinaCheckerStep(deusInMachina);
        var goalTextCheckerStep      = new GoalTextCheckerStep(goalTextChecker);

        // --- Evaluate steps ---
        var storyCompressorStep      = new StoryCompressorStep(storyCompressor);
        var chapterStyleCheckerStep  = new ChapterStyleCheckerStep(chapterStyleChecker);
        var characterCheckerStep     = new CharacterCheckerStep(characterChecker);
        var narrativeArcAnalyzerStep = new NarrativeArcAnalyzerStep(narrativeArcAnalyzer);
        var causalAnalyzerStep       = new CausalAnalyzerStep(causalAnalyzer);

        // --- Workflows ---
        var planWorkflow = new PlanWorkflow(
            chapterPlannerStep, planNarrativeCriticStep, planCoherenceCriticStep,
            goalPlanCheckerStep, engineConfig, log);

        var writeWorkflow = new WriteWorkflow(
            writerStep,
            proofreaderStep, stateExtractorStep, repetitionTrackerStep, repetitionFilterStep,
            sequenceCheckerStep, sequenceStyleCheckerStep,
            textNarrativeCriticStep, textCoherenceCriticStep,
            textDreamCriticStep, textWhatIfCriticStep,
            deusInMachinaStep, goalTextCheckerStep,
            engineConfig, htmlExport, log);

        var evaluateWorkflow = new EvaluateWorkflow(
            storyCompressorStep, chapterStyleCheckerStep, characterCheckerStep,
            narrativeArcAnalyzerStep, causalAnalyzerStep,
            log);

        var orchestrator = new StoryOrchestrator(planWorkflow, writeWorkflow, evaluateWorkflow, htmlExport, log);
        return new RedacteurServiceImpl(scenarioReader, orchestrator);
    }
}
