# 2026-07-12 00h30 — Fix PlanContinuityCritic jamais exécuté + règle "critic sauté"

## 1. Demande

L'utilisateur a remarqué qu'aucun fichier `llm_calls/*_PlanContinuityCritic_*.md` n'existait dans
un run réel (scénario 1998, 6 chapitres), alors que le critic apparaît bien dans les logs avec un
score de 10.0. Diagnostic demandé, puis correctif.

## 2. Diagnostic

`PlanContinuityCritic.call()` court-circuite (retourne un résultat vide/parfait sans appeler le
LLM) quand `input.summary()` est vide. Or `story.summary()` n'est renseigné que par
`ChapterSummaryStep`, appelé depuis `EvaluateWorkflow`, lui-même appelé depuis `StoryOrchestrator`
**après** la boucle Write. Mais `StoryOrchestrator.generate()` exécute `storyPlanWorkflow.run(...)`
(qui planifie l'intégralité des chapitres du livre, via `StoryPlanWorkflow.planEveryChapter`)
**avant** de démarrer la moindre écriture. Donc `story.summary()` est structurellement vide
pendant toute la phase Plan, pour tous les chapitres (pas seulement le premier) : le critic ne
s'exécutait jamais réellement dans un run standard, quel que soit le nombre de chapitres.

Confirmé par l'utilisateur : la bonne donnée à comparer, disponible à ce stade, est le **plan**
déjà généré des chapitres précédents (chaque chapitre est planifié dans l'ordre au sein de la
même boucle), pas un résumé de prose qui n'existe pas encore.

Deuxième point soulevé par l'utilisateur en cours de correction, généralisé en règle : un critic
qui ne s'exécute pas réellement (rien à comparer) ne doit ni apparaître dans les logs, ni entrer
dans le calcul de la moyenne/note éliminatoire — un score 10.0 "gratuit" dilue artificiellement la
moyenne des critics qui, eux, ont vraiment travaillé.

## 3. Ce qui a été touché

### `agent/plan/continuitycritic/PlanContinuityCriticInput.java`
Champ `summary` → `previousChaptersPlans`. Javadoc mise à jour.

### `agent/plan/continuitycritic/PlanContinuityCritic.java`
- Retrait du court-circuit interne (déplacé dans le Step, voir plus bas).
- Prompt (`buildSystem`) : delta présenté et validé avant écriture —
  - `"the story already written"` → `"the earlier chapters already planned for this book"`
  - `"Story summary: what the previous chapters have established."` → `"Earlier chapters' plans: what previous chapters have already set up."`
  - `"From the summary, identify..."` → `"From the earlier chapters' plans, identify..."`
  - `"an open thread the summary presents as pressing"` → `"an open thread an earlier chapter presents as pressing"`
  - Exemple : traduit en anglais au passage (il était resté en français par oubli lors de la
    refonte à 5 axes — incohérent avec le reste du prompt et avec la consigne du projet de
    privilégier l'anglais pour les exemples).
- Slot utilisateur `"Story summary"` → `"Earlier chapters' plans"` (cohérence stricte nom
  mentionné / nom de section, règle VITAL de `agent/CLAUDE.md`).

### `orchestrator/plan/PlanContinuityCriticStep.java`
`run()` retourne désormais `Optional<PlanContinuityCriticOutput>` : `Optional.empty()` quand
aucun chapitre antérieur n'a de plan (premier chapitre du livre). Construit le bloc des plans
précédents à partir de `story.chapters()` (chapitres déjà planifiés dans `scenario.chapters()`
avant le chapitre courant), même convention de formatage que `StoryPlanWorkflow.buildChaptersBlock`
(`"Chapitre <titre> :\n<plan>"`).

### `orchestrator/plan/ChapterPlanWorkflow.java`
- `continuity` devient `Optional<PlanContinuityCriticOutput>`.
- `log.critic("PlanContinuityCritic", ...)` appelé uniquement si présent.
- Moyenne et note éliminatoire calculées sur la liste des scores des critics **ayant réellement
  tourné** (4 au premier chapitre, 5 ensuite) au lieu d'une division fixe par 5 — un critic sauté
  n'influence plus le résultat.
- `mergeFeedback` reçoit une liste vide pour continuity quand absent.
- Javadoc de la classe mise à jour (axe D + règle générale "critic sauté = ni loggé, ni compté").

### `agent/plan/continuitycritic/PlanContinuityCritic.md`
Sections "Rôle", "Historique" (ajout d'un paragraphe correctif daté), "Cas du premier chapitre",
"Ce qu'il vérifie", "Budget de contexte" mises à jour pour refléter la comparaison contre les
plans précédents plutôt que contre un résumé de prose.

## 4. Résultat

`mvn compile` : OK. `mvn test` (tous modules) : **17 (commun) + 5 (testllm) + 35 (redacteur) —
0 échec**. `ChapterPlanWorkflowTest` (fixture 8 chapitres) passe sans modification : le mock
`UNIVERSAL_PASS` couvre le nouveau prompt `PlanContinuityCritic` par défaut (bucket `.otherwise`).
