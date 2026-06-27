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
import storymagine.redacteur.coeur.domaine.agent.chapter.goaltextcritic.GoalTextCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textcoherencecritic.TextCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textdreamcritic.TextDreamCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textnarrativecritic.TextNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.textwhatifcritic.TextWhatIfCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacritic.DeusInMachinaCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.deusinmachinacorrector.DeusInMachinaCorrector;
import storymagine.redacteur.coeur.domaine.agent.writer.checkcritic.CheckCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.planfidelitycritic.PlanFidelityCritic;
import storymagine.redacteur.coeur.domaine.agent.writer.naturalitycorrector.NaturalityCorrector;
import storymagine.redacteur.coeur.domaine.agent.writer.proofreadercorrector.ProofreaderCorrector;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitionfilter.RepetitionFilter;
import storymagine.redacteur.coeur.domaine.agent.writer.repetitiontracker.RepetitionTracker;
import storymagine.redacteur.coeur.domaine.agent.writer.sequencewriter.Writer;
import storymagine.redacteur.coeur.domaine.agent.writer.stateextractor.StateExtractor;
import storymagine.redacteur.coeur.domaine.agent.writer.stylecorrector.StyleCorrector;
import storymagine.redacteur.coeur.domaine.orchestrator.StoryOrchestrator;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.CausalAnalyzerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.ChapterStyleCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.CharacterCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.EvaluateWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.NarrativeArcAnalyzerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.StoryCompressorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.BeatsConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.write.CorrectorConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.ChapterPlannerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.GoalPlanCheckerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.DeusInMachinaCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.DeusInMachinaCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.CheckCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.PlanFidelityCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.GoalTextCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.TextCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.TextDreamCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.TextNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.TextWhatIfCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.NaturalityCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.ProofreaderCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.RepetitionFilterStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.RepetitionTrackerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StateExtractorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StyleCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriteWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriterStep;
import storymagine.redacteur.coeur.ports.HtmlExportPort;
import storymagine.redacteur.coeur.ports.ScenarioReaderPort;
import storymagine.redacteur.coeur.service.RedacteurService;
import storymagine.redacteur.coeur.service.RedacteurServiceImpl;
import storymagine.redacteur.infra.scenario.ScenarioFileAdapter;

/** Wires the redacteur domain with its infrastructure adapters. */
public class RedacteurModule {

    public static RedacteurService assemble(OllamaConfig ollamaConfig, String modelName) {
        LogPort       log = new ConsoleLogAdapter();
        OllamaAdapter llm = ollamaConfig.adapter(modelName, log);
        return assemble(llm, new ScenarioFileAdapter(), log, HtmlExportPort.NOOP, BeatsConfig.defaults());
    }

    public static RedacteurService assemble(OllamaConfig ollamaConfig, String modelName,
                                            LogPort log, HtmlExportPort htmlExport) {
        OllamaAdapter llm = ollamaConfig.adapter(modelName, log);
        return assemble(llm, new ScenarioFileAdapter(), log, htmlExport, BeatsConfig.defaults());
    }

    public static RedacteurService assemble(OllamaConfig ollamaConfig, String modelName,
                                            LogPort log, HtmlExportPort htmlExport, BeatsConfig beatsConfig) {
        OllamaAdapter llm = ollamaConfig.adapter(modelName, log);
        return assemble(llm, new ScenarioFileAdapter(), log, htmlExport, beatsConfig, CorrectorConfig.defaults());
    }

    public static RedacteurService assemble(OllamaConfig ollamaConfig, String modelName,
                                            LogPort log, HtmlExportPort htmlExport,
                                            BeatsConfig beatsConfig, CorrectorConfig correctorConfig) {
        OllamaAdapter llm = ollamaConfig.adapter(modelName, log);
        return assemble(llm, new ScenarioFileAdapter(), log, htmlExport, beatsConfig, correctorConfig);
    }

    public static RedacteurService assemble(OllamaAdapter llm, ScenarioReaderPort scenarioReader) {
        return assemble(llm, scenarioReader, new ConsoleLogAdapter(), HtmlExportPort.NOOP, BeatsConfig.defaults());
    }

    /** Test-friendly overload. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log) {
        return assemble(llm, scenarioReader, log, HtmlExportPort.NOOP, BeatsConfig.defaults());
    }

    /** Full assembly — all parameters explicit. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log, HtmlExportPort htmlExport) {
        return assemble(llm, scenarioReader, log, htmlExport, BeatsConfig.defaults());
    }

    /** Full assembly with custom beats configuration. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log, HtmlExportPort htmlExport, BeatsConfig beatsConfig) {
        return assemble(llm, scenarioReader, log, htmlExport, beatsConfig, CorrectorConfig.defaults());
    }

    /** Full assembly with custom beats and corrector configuration. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log, HtmlExportPort htmlExport,
                                            BeatsConfig beatsConfig, CorrectorConfig correctorConfig) {
        // --- Plan agents ---
        var chapterPlanner      = new ChapterPlanner(llm);
        var planNarrativeCritic = new PlanNarrativeCritic(llm);
        var planCoherenceCritic = new PlanCoherenceCritic(llm);
        var goalPlanChecker     = new GoalPlanChecker(llm);

        // --- Writer agents ---
        var writer                  = new Writer(llm, log);
        var deusInMachinaCritic     = new DeusInMachinaCritic(llm);
        var deusInMachinaCorrector  = new DeusInMachinaCorrector(llm);
        var goalTextCritic          = new GoalTextCritic(llm);
        var proofreaderCorrector    = new ProofreaderCorrector(llm);
        var repetitionFilter        = new RepetitionFilter(llm);
        var repetitionTracker       = new RepetitionTracker(llm);
        var planFidelityCritic      = new PlanFidelityCritic(llm);
        var checkCritic             = new CheckCritic(llm);
        var styleCorrector          = new StyleCorrector(llm);
        var stateExtractor          = new StateExtractor(llm);
        var naturalityCorrector     = new NaturalityCorrector(llm);
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
        var chapterPlannerStep      = new ChapterPlannerStep(chapterPlanner, beatsConfig);
        var planNarrativeCriticStep = new PlanNarrativeCriticStep(planNarrativeCritic);
        var planCoherenceCriticStep = new PlanCoherenceCriticStep(planCoherenceCritic);
        var goalPlanCheckerStep     = new GoalPlanCheckerStep(goalPlanChecker);

        // --- Write steps ---
        var writerStep                  = new WriterStep(writer);
        var deusInMachinaCorrectorStep  = new DeusInMachinaCorrectorStep(deusInMachinaCorrector);
        var naturalityCorrectorStep     = new NaturalityCorrectorStep(naturalityCorrector);
        var proofreaderCorrectorStep    = new ProofreaderCorrectorStep(proofreaderCorrector);
        var deusInMachinaCriticStep     = new DeusInMachinaCriticStep(deusInMachinaCritic);
        var styleCorrectorStep          = new StyleCorrectorStep(styleCorrector);
        var planFidelityCriticStep      = new PlanFidelityCriticStep(planFidelityCritic);
        var checkCriticStep             = new CheckCriticStep(checkCritic);
        var stateExtractorStep          = new StateExtractorStep(stateExtractor);
        var repetitionTrackerStep       = new RepetitionTrackerStep(repetitionTracker);
        var repetitionFilterStep        = new RepetitionFilterStep(repetitionFilter);
        var textNarrativeCriticStep     = new TextNarrativeCriticStep(textNarrativeCritic);
        var textCoherenceCriticStep     = new TextCoherenceCriticStep(textCoherenceCritic);
        var textDreamCriticStep         = new TextDreamCriticStep(textDreamCritic);
        var textWhatIfCriticStep        = new TextWhatIfCriticStep(textWhatIfCritic);
        var goalTextCriticStep          = new GoalTextCriticStep(goalTextCritic);

        // --- Evaluate steps ---
        var storyCompressorStep      = new StoryCompressorStep(storyCompressor);
        var chapterStyleCheckerStep  = new ChapterStyleCheckerStep(chapterStyleChecker);
        var characterCheckerStep     = new CharacterCheckerStep(characterChecker);
        var narrativeArcAnalyzerStep = new NarrativeArcAnalyzerStep(narrativeArcAnalyzer);
        var causalAnalyzerStep       = new CausalAnalyzerStep(causalAnalyzer);

        // --- Workflows ---
        var planWorkflow = new PlanWorkflow(
            chapterPlannerStep, planNarrativeCriticStep, planCoherenceCriticStep,
            goalPlanCheckerStep, log);

        var writeWorkflow = new WriteWorkflow(
            writerStep,
            deusInMachinaCorrectorStep, naturalityCorrectorStep, proofreaderCorrectorStep,
            styleCorrectorStep, deusInMachinaCriticStep, planFidelityCriticStep, checkCriticStep,
            stateExtractorStep, repetitionTrackerStep, repetitionFilterStep,
            textNarrativeCriticStep, textCoherenceCriticStep,
            textDreamCriticStep, textWhatIfCriticStep,
            goalTextCriticStep,
            correctorConfig, htmlExport, log);

        var evaluateWorkflow = new EvaluateWorkflow(
            storyCompressorStep, chapterStyleCheckerStep, characterCheckerStep,
            narrativeArcAnalyzerStep, causalAnalyzerStep,
            log);

        var orchestrator = new StoryOrchestrator(planWorkflow, writeWorkflow, evaluateWorkflow, htmlExport, log);
        return new RedacteurServiceImpl(scenarioReader, orchestrator);
    }
}
