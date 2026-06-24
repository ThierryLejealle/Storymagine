# 2026-06-22 14h30 — Orchestrateur, Workflows et Steps

## Evolution demandée
Créer le package `orchestrator` dans le domaine `redacteur` avec :
- Un package par workflow (plan, write, evaluate)
- Une classe Step par agent dans chaque package workflow
- Un package `common` pour les utilitaires de formatage
- La classe `StoryOrchestrator` comme coordinateur principal

## Ce qui a été touché

### Domaine story — modifications
**`WrittenSequence`** (nouveau record)
- Représente une séquence écrite : `sequencePlan` (SequencePlanner, nullable) + `text` (Writer).

**`WrittenChapter`** (modifié)
- `sequences` : `List<String>` → `List<WrittenSequence>` (tout l'état de génération dans Story).
- `coherence` : ajout du feedback critique accumulé pour les retentatives de planification.
- `fullText()` adapté pour itérer sur `WrittenSequence.text()`.

### Nouveau package `orchestrator/`

**Root**
- `GenerationMode` : enum PLAN_ONLY / DRAFT / STANDARD / FULL avec helpers `runsPlanCritics()`, `runsSequenceCheckers()`, `runsEvaluation()`.
- `GenerationConfig` : record (mode, jsonMode, maxRetries) avec fabriques statiques draft/standard/full.
- `StoryOrchestrator` : itère sur les chapitres du Scenario, délègue à PlanWorkflow → WriteWorkflow → EvaluateWorkflow.

**`common/`**
- `ScenarioFormatters` : helpers statiques Scenario → String (personnages, focusText, loreText, contraintes, checks, descriptions de séquences, bookGoal).
- `StoryFormatters` : helpers statiques Story → String (entityState), et parsing inverse `applyStateLines(WorldState, String)`.

**`plan/`** — 4 Steps + 1 Workflow
- `ChapterPlannerStep` : lit Story (plan courant → isRewrite, coherence → previousPlan/problemsToFix).
- `PlanNarrativeCriticStep`, `PlanCoherenceCriticStep`, `GoalPlanCheckerStep` : lisent `story.currentChapter().plan()`.
- `PlanWorkflow` : boucle maxRetries, accumule le feedback critique dans `wc.setCoherence()`.

**`write/`** — 14 Steps + 1 Workflow
- `SequencePlannerStep` (agent du groupe plan/, utilisé dans WriteWorkflow).
- `WriterStep` : `rewriteProblems=null` → premier essai ; non-null → `isRewrite=true`, problèmes injectés dans chapterPlan.
- `ProofreaderStep`, `StateExtractorStep`, `RepetitionTrackerStep`, `RepetitionFilterStep`.
- `SequenceCheckerStep`, `SequenceStyleCheckerStep`.
- `FocusActionFilterStep` : extrait les groupes du FocusPool.
- `TextNarrativeCriticStep`, `TextCoherenceCriticStep`, `TextDreamCriticStep`, `TextWhatIfCriticStep`.
- `DeusInMachinaCheckerStep`, `GoalTextCheckerStep`.
- `WriteWorkflow` : snapshot/restore WorldState pour chapitres DREAM/WHAT_IF ; boucle séquences ; dispatch des critics par NarrativeType.

**`evaluate/`** — 5 Steps + 1 Workflow
- `StoryCompressorStep` : → `wc.setSummary()` (toujours appelé, même en DRAFT).
- `ChapterStyleCheckerStep`, `CharacterCheckerStep`.
- `NarrativeArcAnalyzerStep`, `CausalAnalyzerStep` : analysent tous les plans disponibles dans Story.
- `EvaluateWorkflow` : StoryCompressor systématique + analysis complète si mode FULL.

## Décisions techniques
- **Step = lecture seule sur Story** ; c'est le Workflow qui écrit dans Story (setPlan, setCoherence, addSequence, setSummary).
- **Aucun WritingContext partagé** : chaque Step fait son propre assemblage spécifique.
- **ScenarioFormatters / StoryFormatters** : helpers statiques partagés pour les conversions communes (personnages, contraintes, entityState…).
- `styleGuide` et `qualityCriteria` passés null pour l'instant (champs non encore sourcés dans le Scenario).
- `loopJournal` dans WriterInput passé null (sera implémenté lors de la migration de l'orchestration).
- `realismLevel` dans TextDreamCriticInput fixé à "moyen" par défaut.

## Résultat
34 classes créées ou modifiées dans `orchestrator/` + `story/`.
Pipeline complet de génération opérationnel : Scenario → Story via PlanWorkflow → WriteWorkflow → EvaluateWorkflow.
