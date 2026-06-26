# Règles de retry — Storymagine

Configuration : enum `QualityLevel` dans `redacteur/coeur/domaine/orchestrator/QualityLevel.java`.
Les valeurs ci-dessous sont celles du niveau `FULL` (le niveau maximal).

---

## 1. Retry du plan (`PlanWorkflow`)

**Déclenchement** : dès qu'au moins un agent critique émet une remarque.

**Nombre de passes** : `1 + planMaxRetry` (FULL : 4 passes, soit 3 retries max).

**Sélection** : à la fin de toutes les passes, le plan avec la **meilleure note moyenne** est retenu.
L'historique des problèmes est passé au planificateur pour guider la correction.

**Agents concernés** : PlanNarrativeCritic, PlanCoherenceCritic, GoalPlanChecker.

---

## 2. Critique par séquence (`WriteWorkflow.runSequenceCritique`)

Phase globale unique : tous les critiques tournent ensemble, une note moyenne est calculée,
et une seule décision de retry est prise pour l'ensemble.

**Seuil de retry** : note moyenne < 7.0 (constante `SEQUENCE_CRITIC_THRESHOLD`).

**Nombre de passes** : `1 + sequenceMaxRetry` (FULL : 3 passes, soit 2 retries max).

**Agents** :

| Agent               | Condition d'activation                        | Score            |
|---------------------|-----------------------------------------------|------------------|
| DeusInMachinaCritic | toujours                                      | algo (fuites)    |
| StyleCritic         | toujours                                      | algo (problèmes) |
| ElementCritic       | si le scénario déclare des `writerChecks`     | algo (ratio)     |

Sur retry : Writer (avec TOUS les problèmes collectés) → Correcteurs → Critics à nouveau.
La **version avec le meilleur score moyen** est retenue.

**Phase Correcteurs** (avant chaque passe Critics, skip si BROUILLON) :
DeusInMachinaCorrector → NaturalityCorrector → ProofreaderCorrector.
Les correcteurs appliquent des patches inline (paires FAUX → JUSTE), jamais de retry Writer.

---

## 3. Critique du chapitre (`WriteWorkflow.runChapterCritique`)

Évalue le **texte complet du chapitre** après toutes les séquences.

**Agents selon le type de chapitre** :

| Type       | Agents                                                         |
|------------|----------------------------------------------------------------|
| IMPERATIVE | TextNarrativeCritic, TextCoherenceCritic, GoalTextCritic      |
| DREAM      | TextDreamCritic, GoalTextCritic                               |
| WHAT_IF    | TextWhatIfCritic, TextCoherenceCritic, GoalTextCritic         |

**Déclenchement** : si la note moyenne ≤ `chapitreThreshold` (FULL : 7.0).

**Nombre de passes** : `1 + chapitreMaxRetry` (FULL : 3 passes, soit 2 retries max).

Sur retry : toutes les séquences sont réécrites (Writer avec feedback chapitre + Correcteurs).
La **version avec le meilleur score moyen** est retenue.

---

## Valeurs par niveau (`QualityLevel`)

| Niveau    | planMaxRetry | sequenceMaxRetry | chapitreMaxRetry | chapitreThreshold |
|-----------|-------------|-----------------|-----------------|-------------------|
| BROUILLON | 0           | 0               | 0               | 7.0               |
| SIMPLE    | 1           | 1               | 1               | 7.0               |
| FULL      | 3           | 2               | 2               | 7.0               |
