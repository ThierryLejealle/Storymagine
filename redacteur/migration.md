# Migration des agents LLM — Redacteur → Storymagine

Date : 2026-06-21 / 2026-06-22
Source : `C:\dev\llm-scriot\Redacteur\src\main\java\story\context\`
Cible  : `redacteur/src/main/java/storymagine/redacteur/coeur/domaine/agent/`

---

## Principes de migration

- Chaque agent devient **une classe POJO** avec un `Input` record, un `Output` record, et un `.md` de documentation.
- Dépendance unique : `ModelCallPort` (port commun → `storymagine.commun.coeur.ports`).
- `LlmContext` (classe mère dans Redacteur) est supprimée : ses appels `llm.generate()` sont délégués directement au port.
- `ContextBudget` / `ContextAllocation` (framework config Redacteur) : remplacés par `llm.contextWindow()` directement dans les agents.
- Jackson (ObjectMapper dans `ScenarioPlannerContext`) : remplacé par un mini-parser JSON inline dans `ChapterPlanner` (pas de framework dans le coeur).
- `CriticContext` contenait **5 méthodes** → décomposé en **5 agents distincts** (2 plan + 2 writer + 1 spécial dream/whatif writer).
- `NarrativeGoalContext` contenait **2 méthodes** → décomposé en **2 agents** (plan + writer).
- `NarrativeCritiqueContext` contenait **2 méthodes** → décomposé en **2 agents** (global).

---

## Table de correspondance — Agents migrés

### Package `agent/plan/` — agents de la phase de planification

| Source Redacteur (`story.context.*`) | Storymagine (`agent.plan.*`) |
|--------------------------------------|------------------------------|
| `ScenarioPlannerContext`             | `ChapterPlanner`             |
| `SequencePlannerContext`             | ~~`SequencePlanner`~~ **supprimé** — agent non-nécessaire : ChapterPlanner JSON produit déjà les plans par séquence via `sequencePlans()` |
| `CriticContext` (méthode plan narratif) | `PlanNarrativeCritic`    |
| `CriticContext` (méthode plan cohérence) | `PlanCoherenceCritic`  |
| `NarrativeGoalContext` (evaluatePlan) | `GoalPlanChecker`           |

> **Note renommage :** `ScenarioPlannerContext` → `ChapterPlanner`
> Le terme "Scénario" était ambigu (désigne aussi le fichier de config). On adopte "Chapter" pour clarifier
> que cet agent planifie **un chapitre** (ses séquences), pas le livre entier.

### Package `agent/writer/` — agents de la boucle de rédaction de séquence

| Source Redacteur (`story.context.*`) | Storymagine (`agent.writer.*`) |
|--------------------------------------|--------------------------------|
| `WriterContext`                      | `Writer`                       |
| `ProofreaderContext`                 | `Proofreader`                  |
| `DeusInMachinaContext`               | `DeusInMachinaChecker`         |
| `SequenceStyleCheckerContext`        | `SequenceStyleChecker`         |
| `SequenceCheckerContext`             | `SequenceChecker`              |
| `RepetitionTrackerContext`           | `RepetitionTracker`            |
| `RepetitionFilterContext`            | `RepetitionFilter`             |
| `StateExtractorContext`              | `StateExtractor`               |
| `LlmFilterContext`                   | `FocusActionFilter`            |
| `CriticContext` (méthode evalNarrative sur texte) | `TextNarrativeCritic` |
| `CriticContext` (méthode evalCoherence sur texte) | `TextCoherenceCritic` |
| `CriticContext` (méthode evalDreamQuality)        | `TextDreamCritic`     |
| `CriticContext` (méthode evalWhatIfPlausibility)  | `TextWhatIfCritic`    |
| `NarrativeGoalContext` (evaluateText)             | `GoalTextChecker`     |

> **Note renommage :** `LlmFilterContext` → `FocusActionFilter`
> Le nom originel était trop générique. Le rôle réel est de filtrer les groupes focus et catégories d'action
> pertinents pour une séquence → d'où `FocusActionFilter`.

### Package `agent/global/` — agents de fin de chapitre / livre

| Source Redacteur (`story.context.*`) | Storymagine (`agent.global.*`) |
|--------------------------------------|--------------------------------|
| `CompressorContext`                  | `StoryCompressor`              |
| `ChapterStyleCheckerContext`         | `ChapterStyleChecker`          |
| `CharacterCheckerContext`            | `CharacterChecker`             |
| `NarrativeCritiqueContext` (evalArcs)  | `NarrativeArcAnalyzer`       |
| `NarrativeCritiqueContext` (evalCausal) | `CausalAnalyzer`            |

### Package `agent/commun/` — parsers partagés (nouveau, sans équivalent direct)

| Storymagine                  | Logique source dans Redacteur                     |
|------------------------------|---------------------------------------------------|
| `ProblemScoreParser`         | Extraite de plusieurs classes (`PROBLEME:/SCORE:`) |
| `CriticOutputParser`         | Extraite de `CriticContext` (format AMELIORATION/DEFAUT tiered) |

---

## Classes Redacteur NON migrées

Ces classes existent dans `story.context` mais **ne sont pas des agents LLM** ou ont été remplacées par le port.

| Classe Redacteur             | Raison de l'exclusion |
|------------------------------|-----------------------|
| `LlmContext`                 | Classe mère technique. Remplacée par injection directe de `ModelCallPort` dans chaque agent. |
| `OptimizerContext`           | Outil de configuration de run (paramètres LLM), pas un agent. |
| `ValidatorContext`           | Validateur de configuration de scénario, pas un agent. |
| `CoherenceCheckerContext`    | Outil de vérification de configuration de cohérence, pas un agent. |
| `StyleCheckerContext`        | Outil de configuration du style checker, pas un agent. |
| `StoryQualityContext`        | Audit post-mortem analytique non-LLM, pas un agent. |

### Autres classes Redacteur à migrer ultérieurement (hors agents)

Ces classes hors `story.context` seront traitées dans des migrations dédiées :

| Package Redacteur | Contenu | Statut |
|-------------------|---------|--------|
| `story.orchestrator` | `ChapterOrchestrator`, `StoryOrchestrator`, `LoopOrchestrator`, `ForeachOrchestrator` | À migrer en service/orchestration |
| `story.memory` | `StoryMemory` | À migrer en domaine ou port |
| `story.model` | Toutes les classes modèle (`Chapter`, `Book`, `SequenceConfig`, etc.) | À migrer en domaine |
| `story.config` | Loaders, resolvers, selectors | À migrer en infra/service |
| `story.llm` | `LlmPort`, `LlmResult`, `RetryPolicy` | En partie déjà dans commun |
| `story.ollama` | `OllamaAdapter` et config | En partie déjà dans commun |
| `story.logging` | `LlmMetrics`, loggers | À migrer en infra |
| `story.checker` | `StoryQualityChecker`, `SemanticChecker`, `PlanCritiqueAgent` | À analyser |

---

## État des agents dans Storymagine

Les agents sont migrés **en structure uniquement**.
Au moment de cette migration :
- Aucun agent n'est relié à un service ou un orchestrateur.
- Aucun objet domaine (`Chapter`, `Sequence`, `Book`) n'existe encore dans Storymagine.
- La connexion agents ↔ orchestration se fera dans une migration ultérieure.
