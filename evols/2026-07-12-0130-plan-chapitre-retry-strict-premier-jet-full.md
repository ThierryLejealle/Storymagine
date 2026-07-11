# 2026-07-12 01h30 — Plan chapitre : relance stricte au premier jet (FULL)

## 1. Demande

En mode qualité FULL, sur la toute première tentative de plan d'un chapitre (le "premier jet"),
relancer systématiquement dès qu'un critic remonte au moins une remarque (amélioration ou défaut),
même si la moyenne et le seuil éliminatoire passent tous les deux. Ne s'applique qu'après le
premier jet (pas aux tentatives suivantes). Si la relance est déclenchée pour cette raison,
l'expliquer dans les logs. Quand un plan est retenu après une relance, préciser le numéro de la
tentative retenue et son score.

## 2. Ce qui a été touché

### `orchestrator/QualityLevel.java`
Nouveau champ `planStrictFirstAttempt` (booléen), 18e paramètre du constructeur — `true`
uniquement pour `FULL`, `false` pour les 3 autres niveaux. Accesseur `planStrictFirstAttempt()`.

### `orchestrator/plan/ChapterPlanWorkflow.java`
Après le calcul de `passed` (moyenne + éliminatoire), ajout d'une condition supplémentaire :
si `planStrictFirstAttempt()` est vrai, qu'on est à la tentative 1 (`attempt == 0`) et qu'au moins
un critic ayant réellement tourné a remonté une remarque (toutes tiers confondus, en excluant
PlanContinuityCritic quand il est sauté — voir le fix précédent), alors `passed` est forcé à
`false`. Un `log.warn` dédié explique la relance uniquement quand c'est la seule cause du rejet
(moyenne/éliminatoire passaient déjà) — même convention que le warn existant pour la note
éliminatoire. La règle ne s'applique jamais aux tentatives suivantes : dès la 2e tentative, seule
la règle habituelle (moyenne/éliminatoire) décide, donc cette règle n'ajoute jamais plus d'une
relance par chapitre.

Le numéro de tentative retenu et son score étaient déjà loggés par le code existant
(`log.planRetained(bestAttempt, finalAttempt, bestScore)`, déclenché dès que `finalAttempt > 1`) —
aucune modification nécessaire, ce mécanisme couvre aussi la nouvelle relance stricte.

Javadoc de la classe mise à jour pour documenter la règle (et corriger une description obsolète
du déclencheur de relance normal, qui ne correspondait déjà plus au code avant ce correctif).

### `orchestrator/CLAUDE.md`
Nouveau paragraphe documentant la règle stricte premier jet, à côté du paragraphe existant sur la
note éliminatoire (même format).

### Tests — `orchestrator/CapturingLogPort.java`
Ajout de la capture des appels `warn()` (`warnings`, liste), absente jusqu'ici — nécessaire pour
vérifier que la relance est bien expliquée dans les logs.

### Tests — `orchestrator/plan/ChapterPlanWorkflowTest.java`
Deux nouveaux tests :
- `full_firstDraftHasARemark_forcesExactlyOneRetry_evenWhenAverageAndEliminationPass` : une seule
  AMELIORATION (score 9.5, très au-dessus des deux seuils FULL) force une relance en FULL ; le
  mock renvoie la même réponse à la 2e tentative (donc même score) — vérifie que la règle ne
  s'applique qu'à la tentative 1 (2 tentatives au total, pas de boucle infinie) ; vérifie le
  message de log explicatif ("premier jet") et le format `planRetained` (tentative retenue +
  score).
- `simple_firstDraftHasARemark_doesNotForceRetry_strictRuleIsFullOnly` : même scénario en SIMPLE,
  aucune relance — garde-fou de portée (la règle est bien réservée à FULL).

## 3. Résultat

`mvn compile` : OK. `mvn test` (tous modules) : **17 (commun) + 5 (testllm) + 37 (redacteur,
dont 2 nouveaux tests) — 0 échec**.
