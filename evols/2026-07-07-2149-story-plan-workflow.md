# 2026-07-07 21h49 53s — StoryPlanWorkflow : planification livre + critique globale + rejeu

## Evolution demandée

Créer un "super workflow plan" qui enchaîne la planification de tous les chapitres d'un livre
(au lieu de planifier chapitre par chapitre, entrelacé avec l'écriture), puis ajoute une étape
de critique globale sur l'ensemble des plans avec rejeu (replanification complète) en cas
d'échec — en réactivant les agents `NarrativeArcAnalyzer` et `CausalAnalyzer`, mis en sommeil
dans `agent/temp/` le 2026-06-27 précisément parce qu'ils tournaient trop tard dans le flux
existant (voir `evols/2026-06-27-2015-desactivation-agents-eval-post-prod.md`).

Décisions validées avec l'utilisateur avant implémentation :
- Rejeu global : replanifie systématiquement TOUS les chapitres (jamais une sélection ciblée).
- Intégration complète immédiate dans `StoryOrchestrator.generate()`, pas de point d'entrée séparé.
- Renommage de l'ancien `PlanWorkflow` (planification d'un seul chapitre) en `ChapterPlanWorkflow`,
  en symétrie avec le nouveau `StoryPlanWorkflow` (planification du livre entier).
- Nouveau package `agent/storyplan/` pour les agents réactivés (au lieu de `agent/global/`, pour
  ne pas mélanger sémantique "avant rédaction" et "après rédaction" comme `StoryCompressor`).

## Ce qui a été touché

**Renommage** :
- `orchestrator/plan/PlanWorkflow.java` → `orchestrator/plan/ChapterPlanWorkflow.java` (classe
  renommée, logique interne inchangée).

**Domaine `story`** :
- `Story.java` : ajout de `activateChapter(ChapterId)` pour réactiver un chapitre déjà planifié
  lors de la phase d'écriture. Introduction d'un pointeur `currentIndex` explicite (l'ancienne
  implémentation de `currentChapter()` se basait sur "le dernier élément de la liste", ce qui
  devenait faux dès que tous les chapitres sont créés en phase 1 puis réactivés un par un en
  phase 2 — bug détecté et corrigé avant tout test).

**Agents réactivés** (déplacés de `agent/temp/` vers `agent/storyplan/`, prompts inchangés) :
- `NarrativeArcAnalyzer` + Input/Output/`.md`
- `CausalAnalyzer` + Input/Output/`.md`
- Correction au passage d'un mojibake d'encodage (`â€”` → `—`) présent dans les commentaires
  Javadoc des classes Input/Output ; vérification systématique d'absence de BOM après écriture.

**Nouveau code** :
- `orchestrator/storyplan/NarrativeArcAnalyzerStep.java`, `CausalAnalyzerStep.java`.
- `orchestrator/storyplan/StoryPlanWorkflow.java` : planifie chaque chapitre via
  `ChapterPlanWorkflow` (inchangé), puis critique globale (si `runsBookCritics`), puis rejeu
  complet si échec, avec conservation du meilleur jeu de plans (snapshot privé par tentative).
- `QualityLevel.java` : 4 nouveaux champs (`runsBookCritics`, `bookMaxRetry`,
  `bookAverageThreshold`, `bookEliminationThreshold`). Activé uniquement au niveau `FULL`
  (2 retries, seuils 8.0 / 5.5 — alignés sur les seuils plan chapitre `FULL`).
- `StoryOrchestrator.java` : restructuré en 2 phases — `StoryPlanWorkflow.run()` une fois pour
  tout le livre, puis boucle write/evaluate par chapitre (inchangée à part
  `startChapter` → `activateChapter`).
- `RedacteurModule.java` : câblage des nouveaux agents/steps/workflow.

**Tests** :
- `StoryPlanWorkflowTest.java` (3 cas) : critique globale qui passe du premier coup (chaque
  chapitre planifié une fois) ; critique globale qui échoue puis réussit (rejeu qui replanifie
  bien TOUS les chapitres, pas une sélection — assertion sur le nombre d'appels `ChapterPlanner`) ;
  niveau `SIMPLE` où `runsBookCritics=false` (critique globale absente, chapitres quand même
  planifiés).
- `CapturingLogPort.java` (test double partagé) : ajout de `planRetentions` (capture
  `planRetained`) et `countPhase(fragment)`, réutilisables par d'autres tests de workflow.

**Documentation** :
- `orchestrator/CLAUDE.md` : nouveau schéma texte pour `StoryPlanWorkflow`, table des seuils
  mise à jour avec la colonne "Plan livre".
- `specs/todo.md` : suppression de l'entrée `NarrativeArcAnalyzer + CausalAnalyzer` de la section
  "Agents en attente" (traitée).
- `specs/retry-rules.md` : nouvelle section "0. Retry du plan livre", renommage des références
  `PlanWorkflow` → `ChapterPlanWorkflow`, tableau des seuils étendu.

## Résultat

`mvn clean compile` et `mvn test` (build complet, 4 modules) : succès, 19 tests dans `redacteur`
dont les 3 nouveaux, aucune régression sur les tests existants (`WorkflowLogTest`,
`ScenarioLoadTest`, `LoreItemParserTest`).

Hors scope (volontairement non traité, cf. `specs/todo.md`) : `ChapterStyleChecker` et
`CharacterChecker` restent en sommeil dans `agent/temp/`. Aucun prompt d'agent n'a été modifié.
