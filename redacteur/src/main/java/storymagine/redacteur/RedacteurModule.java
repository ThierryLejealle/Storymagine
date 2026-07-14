package storymagine.redacteur;

import storymagine.commun.coeur.ports.LogPort;
import storymagine.commun.coeur.ports.ModelCallPort;
import storymagine.commun.infra.ConsoleLogAdapter;
import storymagine.commun.infra.OllamaAdapter;
import storymagine.commun.infra.OllamaConfig;
import storymagine.redacteur.coeur.domaine.agent.global.chaptersummarizer.ChapterSummarizer;
import storymagine.redacteur.coeur.domaine.agent.global.summarycompressor.SummaryCompressor;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storycausalcritic.StoryCausalCritic;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storyfidelitycritic.StoryFidelityCritic;
import storymagine.redacteur.coeur.domaine.agent.storyplan.storynarrativecritic.StoryNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.chapterplanner.ChapterPlanner;
import storymagine.redacteur.coeur.domaine.agent.plan.goalcritic.PlanGoalCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.factscritic.PlanFactsCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.coherencecritic.PlanCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.continuitycritic.PlanContinuityCritic;
import storymagine.redacteur.coeur.domaine.agent.plan.dramacritic.PlanDramaCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.goalcritic.ChapterGoalCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.coherencecritic.ChapterCoherenceCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.dreamcritic.ChapterDreamCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.narrativecritic.ChapterNarrativeCritic;
import storymagine.redacteur.coeur.domaine.agent.chapter.whatifcritic.ChapterWhatIfCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacritic.DeusInMachinaCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.deusinmachinacorrector.DeusInMachinaCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.checkcritic.CheckCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.planfidelitycritic.PlanFidelityCritic;
import storymagine.redacteur.coeur.domaine.agent.sequence.naturalitycorrector.NaturalityCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.grammarcorrector.GrammarCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.phraseextractor.PhraseExtractor;
import storymagine.redacteur.coeur.domaine.agent.sequence.phrasingcorrector.PhrasingCorrector;
import storymagine.redacteur.coeur.domaine.agent.sequence.repetitionfilter.RepetitionFilter;
import storymagine.redacteur.coeur.domaine.agent.sequence.repetitiontracker.RepetitionTracker;
import storymagine.redacteur.coeur.domaine.agent.sequence.writer.Writer;
import storymagine.redacteur.coeur.domaine.agent.sequence.stateextractor.StateExtractor;
import storymagine.redacteur.coeur.domaine.agent.sequence.stylecorrector.StyleCorrector;
import storymagine.redacteur.coeur.domaine.orchestrator.StoryOrchestrator;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.EvaluateWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.evaluate.ChapterSummaryStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.BeatsConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.write.CorrectorConfig;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.ChapterPlannerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanGoalCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanFactsCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanContinuityCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.PlanDramaCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.plan.ChapterPlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.storyplan.StoryCausalCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.storyplan.StoryFidelityCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.storyplan.StoryNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.storyplan.StoryPlanWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.DeusInMachinaCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.DeusInMachinaCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.CheckCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.PlanFidelityCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterGoalCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterCoherenceCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterDreamCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterNarrativeCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.chapter.ChapterWhatIfCriticStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.NaturalityCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.GrammarCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.PhraseExtractorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.PhrasingCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.RepetitionFilterStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.RepetitionTrackerStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StateExtractorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.StyleCorrectorStep;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriteWorkflow;
import storymagine.redacteur.coeur.domaine.orchestrator.write.WriterStep;
import storymagine.redacteur.coeur.ports.CheckpointPort;
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
        return assemble(llm, scenarioReader, log, htmlExport, CheckpointPort.NOOP, beatsConfig, correctorConfig);
    }

    /** Full assembly with custom beats, corrector and checkpoint configuration. */
    public static RedacteurService assemble(ModelCallPort llm, ScenarioReaderPort scenarioReader,
                                            LogPort log, HtmlExportPort htmlExport, CheckpointPort checkpoint,
                                            BeatsConfig beatsConfig, CorrectorConfig correctorConfig) {
        // --- Plan agents ---
        var chapterPlanner       = new ChapterPlanner(llm, log);
        var planGoalCritic       = new PlanGoalCritic(llm, log);
        var planFactsCritic      = new PlanFactsCritic(llm, log);
        var planCoherenceCritic  = new PlanCoherenceCritic(llm, log);
        var planContinuityCritic = new PlanContinuityCritic(llm, log);
        var planDramaCritic      = new PlanDramaCritic(llm, log);

        // --- Writer agents ---
        var writer                  = new Writer(llm, log);
        var deusInMachinaCritic     = new DeusInMachinaCritic(llm);
        var deusInMachinaCorrector  = new DeusInMachinaCorrector(llm);
        var goalTextCritic          = new ChapterGoalCritic(llm, log);
        var grammarCorrector        = new GrammarCorrector(llm, log);
        var phrasingCorrector       = new PhrasingCorrector(llm, log);
        var repetitionFilter        = new RepetitionFilter(llm);
        var repetitionTracker       = new RepetitionTracker(llm, log);
        var planFidelityCritic      = new PlanFidelityCritic(llm, log);
        var checkCritic             = new CheckCritic(llm, log);
        var styleCorrector          = new StyleCorrector(llm, log);
        var phraseExtractor         = new PhraseExtractor(llm, log);
        var stateExtractor          = new StateExtractor(llm, log);
        var naturalityCorrector     = new NaturalityCorrector(llm, log);
        var textCoherenceCritic  = new ChapterCoherenceCritic(llm, log);
        var textDreamCritic      = new ChapterDreamCritic(llm, log);
        var textNarrativeCritic  = new ChapterNarrativeCritic(llm, log);
        var textWhatIfCritic     = new ChapterWhatIfCritic(llm, log);

        // --- Global agents ---
        var chapterSummarizer = new ChapterSummarizer(llm, log);
        var summaryCompressor = new SummaryCompressor(llm);

        // --- Story plan agents ---
        var storyFidelityCritic  = new StoryFidelityCritic(llm, log);
        var storyNarrativeCritic = new StoryNarrativeCritic(llm, log);
        var storyCausalCritic    = new StoryCausalCritic(llm, log);

        // --- Plan steps ---
        var chapterPlannerStep       = new ChapterPlannerStep(chapterPlanner, beatsConfig);
        var planGoalCriticStep       = new PlanGoalCriticStep(planGoalCritic);
        var planFactsCriticStep      = new PlanFactsCriticStep(planFactsCritic);
        var planCoherenceCriticStep  = new PlanCoherenceCriticStep(planCoherenceCritic);
        var planContinuityCriticStep = new PlanContinuityCriticStep(planContinuityCritic);
        var planDramaCriticStep      = new PlanDramaCriticStep(planDramaCritic);

        // --- Story plan steps ---
        var storyFidelityCriticStep  = new StoryFidelityCriticStep(storyFidelityCritic);
        var storyNarrativeCriticStep = new StoryNarrativeCriticStep(storyNarrativeCritic);
        var storyCausalCriticStep    = new StoryCausalCriticStep(storyCausalCritic);

        // --- Write steps ---
        var writerStep                  = new WriterStep(writer);
        var deusInMachinaCorrectorStep  = new DeusInMachinaCorrectorStep(deusInMachinaCorrector);
        var naturalityCorrectorStep     = new NaturalityCorrectorStep(naturalityCorrector);
        var grammarCorrectorStep        = new GrammarCorrectorStep(grammarCorrector);
        var phrasingCorrectorStep       = new PhrasingCorrectorStep(phrasingCorrector);
        var deusInMachinaCriticStep     = new DeusInMachinaCriticStep(deusInMachinaCritic);
        var styleCorrectorStep          = new StyleCorrectorStep(styleCorrector);
        var phraseExtractorStep         = new PhraseExtractorStep(phraseExtractor);
        var planFidelityCriticStep      = new PlanFidelityCriticStep(planFidelityCritic);
        var checkCriticStep             = new CheckCriticStep(checkCritic);
        var stateExtractorStep          = new StateExtractorStep(stateExtractor);
        var repetitionTrackerStep       = new RepetitionTrackerStep(repetitionTracker);
        var repetitionFilterStep        = new RepetitionFilterStep(repetitionFilter);
        var textNarrativeCriticStep     = new ChapterNarrativeCriticStep(textNarrativeCritic);
        var textCoherenceCriticStep     = new ChapterCoherenceCriticStep(textCoherenceCritic);
        var textDreamCriticStep         = new ChapterDreamCriticStep(textDreamCritic);
        var textWhatIfCriticStep        = new ChapterWhatIfCriticStep(textWhatIfCritic);
        var goalTextCriticStep          = new ChapterGoalCriticStep(goalTextCritic);

        // --- Evaluate steps ---
        var chapterSummaryStep = new ChapterSummaryStep(chapterSummarizer, summaryCompressor, stateExtractorStep);

        // --- Workflows ---
        var chapterPlanWorkflow = new ChapterPlanWorkflow(
            chapterPlannerStep, planGoalCriticStep, planFactsCriticStep, planCoherenceCriticStep,
            planContinuityCriticStep, planDramaCriticStep, log);

        var storyPlanWorkflow = new StoryPlanWorkflow(
            chapterPlanWorkflow, storyFidelityCriticStep, storyNarrativeCriticStep, storyCausalCriticStep, log);

        var writeWorkflow = new WriteWorkflow(
            writerStep,
            phrasingCorrectorStep,
            deusInMachinaCorrectorStep, naturalityCorrectorStep, grammarCorrectorStep,
            styleCorrectorStep, phraseExtractorStep, deusInMachinaCriticStep, planFidelityCriticStep, checkCriticStep,
            stateExtractorStep, repetitionTrackerStep, repetitionFilterStep,
            textNarrativeCriticStep, textCoherenceCriticStep,
            textDreamCriticStep, textWhatIfCriticStep,
            goalTextCriticStep,
            correctorConfig, htmlExport, log);

        var evaluateWorkflow = new EvaluateWorkflow(chapterSummaryStep, log);

        var orchestrator = new StoryOrchestrator(
            storyPlanWorkflow, writeWorkflow, evaluateWorkflow, htmlExport, checkpoint, log);
        return new RedacteurServiceImpl(scenarioReader, orchestrator);
    }
}
