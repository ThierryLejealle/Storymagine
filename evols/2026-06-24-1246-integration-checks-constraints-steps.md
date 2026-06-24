# 2026-06-24 12h46 — Intégration checks/constraints chapitre+séquence dans les steps

## Évolution demandée
Brancher les checks et contraintes des niveaux chapitre et séquence (introduits dans l'évol précédente)
dans tous les steps de génération et de critique, via une logique additive
scenario → chapter → sequence.

## Ce qui a été touché

### ScenarioFormatters (nouvelles surcharges)
Six méthodes ajoutées pour merger les niveaux :
- `planConstraints(Scenario, Chapter)` — merge scenario + chapter (phase plan)
- `planChecks(Scenario, Chapter)` — idem pour les checks
- `writerConstraints(Scenario, Chapter)` — merge scenario + chapter (critics niveau chapitre)
- `writerChecks(Scenario, Chapter)` — idem
- `writerConstraints(Scenario, Chapter, Sequence)` — merge des trois niveaux (séquence)
- `writerChecks(Scenario, Chapter, Sequence)` — idem

### Steps plan
- `ChapterPlannerStep` : planConstraints(scenario.constraints()) → planConstraints(scenario, chapter)
- `PlanCoherenceCriticStep` : idem pour checks et constraints

### Steps write
- `WriterStep` : writerConstraints → writerConstraints(scenario, chapter, sequence)
- `SequenceCheckerStep` : signature `(Sequence, String, CheckList)` → `(Sequence, String, Scenario, Chapter)` ; le merge est fait dans le formatter
- `TextCoherenceCriticStep` : writerChecks + writerConstraints → surcharges avec chapter
- `TextWhatIfCriticStep` : ajout param `Chapter` → writerConstraints(scenario, chapter)
- `DeusInMachinaCheckerStep` : idem

### WriteWorkflow
- Appel `deusInMachinaCheckerStep.run(text, scenario)` → ajout `chapter`
- Appel `textWhatIfCriticStep.run(fullText, scenario)` → ajout `chapter`
- Appel `sequenceCheckerStep.run(seq, text, scenario.checks())` → `(seq, text, scenario, chapter)`
- Garde du SequenceChecker : `scenario.checks().writerChecks().isEmpty()` → `ScenarioFormatters.writerChecks(scenario, chapter, seq).isEmpty()`

## Résultat
Les checks et contraintes déclarés au niveau chapitre ou séquence sont maintenant
transmis à tous les agents. La logique additive est uniforme.
Aucune nouvelle erreur de compilation (les erreurs LogPort/LlmCallContext sont pré-existantes).
