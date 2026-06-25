# 2026-06-25 14h30 - Introduction de QualityLevel

## Evolution demandee

Introduire un niveau de qualite de generation configurable via une enumeration
portant tous les parametres d'orchestration, afin de pouvoir ajouter de nouveaux
niveaux sans modifier le code des workflows.

## Ce qui a ete touche

### Fichier cree
- `redacteur/src/main/java/.../orchestrator/QualityLevel.java`
  Enum a 4 niveaux (PLAN_ONLY, BROUILLON, SIMPLE, FULL) portant 9 parametres :
  runsWriting, runsPlanCritics, runsProofreader, runsSequenceCheckers, runsEvaluation,
  planMaxRetry, sequenceMaxRetry, chapitreMaxRetry, chapitreThreshold.

### Fichiers supprimes
- `GenerationMode.java` — remplace par QualityLevel
- `EngineConfig.java` — parametres absorbes dans l'enum
- `EngineConfigLoader.java` — lecture de engine.properties devenue inutile

### Fichiers modifies
- `GenerationConfig.java` — `mode` -> `qualityLevel` ; factories : `brouillon()`, `simple()`, `full()`, `planOnly()`
- `PlanWorkflow.java` — suppression de EngineConfig ; lecture via `config.qualityLevel()`
- `WriteWorkflow.java` — idem + ajout du garde `runsProofreader` sur le Proofreader
- `EvaluateWorkflow.java` — `config.mode()` -> `config.qualityLevel()`
- `StoryOrchestrator.java` — `mode != PLAN_ONLY` -> `qualityLevel.runsWriting()`
- `RedacteurModule.java` — suppression de EngineConfig/EngineConfigLoader dans l'assemblage
- `RedacteurCli.java` — menu BROUILLON/SIMPLE/FULL ; affichage `qualityLevel`
- `WorkflowLogTest.java` — factories mises a jour

## Resultat

Compilation OK (mvn compile sans erreur).
Ajouter un niveau = ajouter une constante dans QualityLevel, zero changement ailleurs.

### Parametres par niveau

| Niveau    | Critics plan | Proofreader | Checkers seq | Evaluation | Retry plan | Retry seq | Retry chap |
|-----------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| PLAN_ONLY | -   | -   | -   | -   | 0 | 0 | 0 |
| BROUILLON | -   | -   | -   | -   | 0 | 0 | 0 |
| SIMPLE    | oui | oui | oui | -   | 1 | 1 | 1 |
| FULL      | oui | oui | oui | oui | 4 | 2 | 2 |
