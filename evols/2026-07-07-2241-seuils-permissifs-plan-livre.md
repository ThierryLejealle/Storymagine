# 2026-07-07 22h41 47s — Seuils très permissifs pour la critique plan livre

## Evolution demandée

Suite à l'ajout de `StoryPlanWorkflow` (voir `evols/2026-07-07-2149-story-plan-workflow.md`),
l'utilisateur a demandé de baisser fortement les seuils de la nouvelle boucle de critique
"plan livre" (`NarrativeArcAnalyzer` + `CausalAnalyzer`) tant que ces deux agents ne sont pas
éprouvés en usage réel : il ne veut pas qu'ils bloquent des générations pour l'instant.

Valeurs demandées : `bookAverageThreshold=1.0` (seuil déclenchant un retry) et
`bookEliminationThreshold=0.0` (note éliminatoire), à la place des 8.0 / 5.5 initiaux (alignés
sur le plan chapitre `FULL`), et ce partout où la colonne existe dans `QualityLevel` (les 3
autres niveaux ont `runsBookCritics=false`, donc ces valeurs y restent inertes — clarifié
explicitement avec l'utilisateur avant modification).

## Ce qui a été touché

- `QualityLevel.java` : `bookAverageThreshold` et `bookEliminationThreshold` passés à `1.0`/`0.0`
  sur les 4 niveaux (`PLAN_ONLY`, `BROUILLON`, `SIMPLE`, `FULL`). `runsBookCritics` et
  `bookMaxRetry` inchangés (seul `FULL` a la boucle active, `bookMaxRetry=2`).
- `StoryPlanWorkflowTest.java` : le scénario "critique échoue puis réussit" utilisait un score
  simulé de 3/10, insuffisant pour échouer sous le nouveau seuil moyen de 1.0 — remplacé par un
  score de 0/10 pour continuer à exercer le chemin de rejeu.
- Bug de test découvert et corrigé au passage : le mock scripté du test cherchait le fragment
  `"analyste de coherence narrative"` (sans accent) pour identifier les appels à `CausalAnalyzer`,
  alors que le prompt réel dit `"analyste de cohérence narrative"` (avec accent) — la branche ne
  matchait donc jamais et retombait silencieusement sur la réponse de secours (SCORE: 10),
  masquant le scénario d'échec voulu par le test. Corrigé dans `StoryPlanWorkflowTest.java`
  (les deux occurrences, y compris celle du premier test qui fonctionnait par coïncidence car
  le fallback donnait aussi SCORE: 10).
- `orchestrator/CLAUDE.md` : table des seuils par niveau mise à jour (1.0 / 0.0 pour FULL) +
  note expliquant le caractère volontairement permissif.
- `specs/retry-rules.md` : section "0. Retry du plan livre" et tableau des seuils mis à jour
  avec les nouvelles valeurs.

## Résultat

`mvn -pl redacteur -am clean test` : succès, 19 tests, aucune régression. Aucun prompt d'agent
modifié — seuls des paramètres numériques de `QualityLevel` (code Java, pas prompt LLM).
