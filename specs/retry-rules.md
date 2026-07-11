# Règles de retry — Storymagine

Configuration : enum `QualityLevel` dans `redacteur/coeur/domaine/orchestrator/QualityLevel.java`.
Les valeurs ci-dessous sont celles du niveau `FULL` (le niveau maximal).

---

## 0. Retry du plan livre (`StoryPlanWorkflow`)

Évalue, pour **chaque chapitre**, sa **consigne d'auteur** (description + objectif) suivie de
son **plan généré**, une fois que chaque chapitre a été planifié une première fois via
`ChapterPlanWorkflow` (voir règle 1). Actif uniquement si `runsBookCritics` est vrai (niveau
FULL uniquement à ce jour). La consigne fait toujours foi et n'est jamais elle-même un défaut —
`StoryFidelityCritic` vérifie que le plan la respecte, `StoryNarrativeCritic`/`StoryCausalCritic`
ne jugent que ce que le plan ajoute au-delà d'elle (voir leur documentation `.md`).

**Déclenchement** : note moyenne < `bookAverageThreshold`, ou un des trois critiques sous
`bookEliminationThreshold`.

**Seuils volontairement très permissifs pour l'instant** (FULL : `bookAverageThreshold=1.0`,
`bookEliminationThreshold=0.0`) — les trois agents ne sont pas encore éprouvés en usage réel, on
ne veut pas qu'ils bloquent une génération. Avec ces valeurs, le rejeu est aujourd'hui
**structurellement inatteignable** : `CriticOutputParser.calculateScore()` (format à 3 paliers)
ne descend jamais sous 1.0, donc `avg ≥ 1.0` est toujours vrai — voulu, pas un bug. À resserrer
une fois le comportement des critics observé sur du contenu concret.

**Nombre de passes** : `1 + bookMaxRetry` (FULL : 3 passes, soit 2 retries max — inatteignable
pour l'instant, voir ci-dessus).

**Sélection** : à la fin de toutes les passes, le jeu de plans (tous chapitres) avec la
**meilleure note moyenne livre** est retenu.

**Portée du rejeu** : replanifie **tous les chapitres**, jamais une sélection ciblée — même
principe que les règles 2 et 3 ci-dessous. Le feedback fusionné des trois critiques est déposé
dans `WrittenChapter.coherence()` de chaque chapitre avant la replanification ; comme
`WrittenChapter.plan()` n'est plus `null`, `ChapterPlannerStep` traite automatiquement chaque
chapitre comme une réécriture guidée par ce feedback.

**Agents concernés** : `StoryFidelityCritic`, `StoryNarrativeCritic`, `StoryCausalCritic`
(package `agent/storyplan/`).

---

## 1. Retry du plan chapitre (`ChapterPlanWorkflow`)

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
DeusInMachinaCorrector → NaturalityCorrector → StyleCorrector → GrammarCorrector.
Les correcteurs appliquent des patches inline (paires FAUX → JUSTE), jamais de retry Writer.

Chaque correcteur expose sa propre `RetryStrategy` (voir `agent/CLAUDE.md`) pour décider s'il se
relance sur sa propre sortie : `RATIO_THRESHOLD` (Deus/Naturality/Style, comportement historique) ou
`DECREASING_AND_RATIO_THRESHOLD` (GrammarCorrector — ne se relance que si le nombre de fautes dépasse
encore le seuil ET diminue d'une passe à l'autre).

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

| Niveau    | planMaxRetry | sequenceMaxRetry | chapitreMaxRetry | chapitreThreshold | bookMaxRetry | runsBookCritics | bookThreshold / bookElimin |
|-----------|-------------|-----------------|-----------------|-------------------|--------------|-----------------|------------------------------|
| BROUILLON | 0           | 0               | 0               | 7.0               | 0            | non             | 1.0 / 0.0 (jamais utilisé)   |
| SIMPLE    | 1           | 1               | 1               | 7.0               | 0            | non             | 1.0 / 0.0 (jamais utilisé)   |
| FULL      | 3           | 2               | 2               | 7.0               | 2            | oui             | 1.0 / 0.0                    |
