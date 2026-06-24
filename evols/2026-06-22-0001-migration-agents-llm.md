# 2026-06-22 00h01 — Migration des agents LLM Redacteur → Storymagine

## Evolution demandée
Migrer l'ensemble des agents LLM du projet Redacteur (`story.context.*`) vers le module
`redacteur` de Storymagine, dans `coeur/domaine/agent/`, en respectant l'architecture hexagonale
et les principes DDD du projet.

## Ce qui a été touché

### Nouveau port
- `commun/coeur/ports/ModelCallPort.java` : ajout de `int contextWindow()`
  (OllamaAdapter disposait déjà de la méthode — pas de modification côté adapteur)

### Nouveaux packages créés dans `redacteur/coeur/domaine/agent/`

**`agent/commun/`** — parsers partagés
- `ProblemScoreParser` : parse PROBLEME:/SCORE: dans les réponses LLM
- `CriticOutputParser` : parse le format tiered AMELIORATION/DEFAUT_SIGNIFICATIF/DEFAUT_MAJEUR

**`agent/plan/`** — agents de planification de chapitre (5 agents)
- `ChapterPlanner` + Input/Output + `ChapterPlanner.md`
- `SequencePlanner` + Input/Output + `SequencePlanner.md`
- `PlanNarrativeCritic` + Input/Output + `PlanNarrativeCritic.md`
- `PlanCoherenceCritic` + Input/Output + `PlanCoherenceCritic.md`
- `GoalPlanChecker` + Input/Output + `GoalPlanChecker.md`

**`agent/writer/`** — agents de la boucle de rédaction de séquence (14 agents)
- `Writer` + Input/Output + `Writer.md`
- `Proofreader` + Input/Output + `Proofreader.md`
- `DeusInMachinaChecker` + Input/Output + `DeusInMachinaChecker.md`
- `SequenceStyleChecker` + Input/Output + `SequenceStyleChecker.md`
- `SequenceChecker` + Input/Output + `SequenceChecker.md`
- `RepetitionTracker` + Input/Output + `RepetitionTracker.md`
- `RepetitionFilter` + Input/Output + `RepetitionFilter.md`
- `StateExtractor` + Input/Output + `StateExtractor.md`
- `FocusActionFilter` + Input/Output + `FocusActionFilter.md`
- `TextNarrativeCritic` + Input/Output + `TextNarrativeCritic.md`
- `TextCoherenceCritic` + Input/Output + `TextCoherenceCritic.md`
- `TextDreamCritic` + Input/Output + `TextDreamCritic.md`
- `TextWhatIfCritic` + Input/Output + `TextWhatIfCritic.md`
- `GoalTextChecker` + Input/Output + `GoalTextChecker.md`

**`agent/global/`** — agents de fin de chapitre / livre (5 agents)
- `StoryCompressor` + Input/Output + `StoryCompressor.md`
- `ChapterStyleChecker` + Input/Output + `ChapterStyleChecker.md`
- `CharacterChecker` + Input/Output + `CharacterChecker.md`
- `NarrativeArcAnalyzer` + Input/Output + `NarrativeArcAnalyzer.md`
- `CausalAnalyzer` + Input/Output + `CausalAnalyzer.md`

### Nouveau fichier de référence
- `redacteur/migration.md` : table de correspondance bidirectionnelle entre les classes
  Redacteur (`story.context.*`) et les agents Storymagine. Inclut les classes non migrées et
  les modules Redacteur restant à traiter.

## Décisions techniques

- `CriticContext` (5 méthodes) → 5 agents distincts : pas de mutualisation entre plan et writer,
  pas de classe abstraite partagée.
- `ScenarioPlannerContext` → renommé `ChapterPlanner` (clarté DDD : plan d'un chapitre).
- `LlmFilterContext` → renommé `FocusActionFilter` (nom métier explicite).
- `ContextBudget`/`ContextAllocation` (framework Redacteur) → remplacés par `llm.contextWindow()`
  directement dans chaque agent.
- Jackson (ObjectMapper) absent du coeur → mini-parser JSON inline dans `ChapterPlanner`.
- `LlmContext` (classe mère Redacteur) → supprimée : injection directe de `ModelCallPort`.

## Résultat
24 agents migrés (structure uniquement — non reliés à l'orchestration).
Les agents n'ont aucune dépendance vers le domaine (Scenario, Chapter, Sequence) :
ils seront connectés lors de la migration de l'orchestration.
