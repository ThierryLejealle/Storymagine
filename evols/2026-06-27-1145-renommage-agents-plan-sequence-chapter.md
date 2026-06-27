# 2026-06-27 11h45 — Renommage des AGENT_NAME : préfixes Plan/Sequence/Chapter

## Evolution demandée
Règle : les agents opérant sur le PLAN sont préfixés "Plan", ceux sur la séquence
"Sequence", ceux sur le chapitre "Chapter". Seule la constante AGENT_NAME est modifiée
(pas les packages ni les classes).

## Ce qui a été touché
17 constantes AGENT_NAME dans les classes agent + strings en dur dans les orchestrateurs
et le test :

Sequence (étaient sans préfixe dans writer/) :
- Writer → SequenceWriter
- DeusInMachinaCorrector → SequenceDeusInMachinaCorrector
- NaturalityCorrector → SequenceNaturalityCorrector
- StyleCorrector → SequenceStyleCorrector
- ProofreaderCorrector → SequenceProofreaderCorrector
- DeusInMachinaCritic → SequenceDeusInMachinaCritic
- PlanFidelityCritic → SequencePlanFidelityCritic
- CheckCritic → SequenceCheckCritic
- StateExtractor → SequenceStateExtractor
- RepetitionTracker → SequenceRepetitionTracker
- RepetitionFilter → SequenceRepetitionFilter

Chapter (étaient préfixés "Text" dans writer/) :
- TextNarrativeCritic → ChapterNarrativeCritic
- TextCoherenceCritic → ChapterCoherenceCritic
- TextDreamCritic → ChapterDreamCritic
- TextWhatIfCritic → ChapterWhatIfCritic
- GoalTextCritic → ChapterGoalCritic

Plan (réordonnancement du préfixe) :
- GoalPlanChecker → PlanGoalChecker

Orchestrateurs mis à jour : WriteWorkflow, PlanWorkflow
Test mis à jour : WorkflowLogTest

## Résultat
Les logs et noms de fichiers générés reflètent clairement le niveau opérationnel de chaque agent.
