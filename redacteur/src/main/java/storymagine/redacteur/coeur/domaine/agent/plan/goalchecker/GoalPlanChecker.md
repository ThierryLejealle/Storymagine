# GoalPlanChecker

## Rôle
Évalue si un plan de chapitre remplit son objectif narratif spécifique (le "but du chapitre").
Distinct des critiques : les critiques évaluent la qualité/cohérence, ce checker évalue la fonction narrative.

## Format de sortie
```
PROBLEME: [défaut ou axe d'amélioration]
...
```
ou `PROBLEME: [RIEN]` si aucun problème trouvé.

## Score — calculé en Java (pas par le LLM)
| Problèmes | Score |
|-----------|-------|
| 0  | 10 |
| 1  | 8  |
| 2  | 6  |
| 3  | 5  |
| 4  | 4  |
| 5  | 3  |
| 6  | 2  |
| 7+ | 1  |

Méthode : `GoalPlanChecker.scoreFromProblemCount(int)`.

## Parsing
Utilise `ProblemScoreParser.parseProblems()` (tag PROBLEME:). Le score n'est pas parsé depuis la réponse LLM.

## Différence avec les critics
- Critics → qualité/cohérence (AMELIORATION/DEFAUT tiers)
- GoalPlanChecker → fonction narrative (PROBLEME/SCORE)

## Source Redacteur
`story.context.NarrativeGoalContext.evaluatePlan`
