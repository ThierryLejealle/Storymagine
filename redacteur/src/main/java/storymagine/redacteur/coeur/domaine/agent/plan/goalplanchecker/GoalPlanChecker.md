# GoalPlanChecker

## Rôle
Évalue si un plan de chapitre remplit son objectif narratif spécifique (le "but du chapitre").
Distinct des critiques : les critiques évaluent la qualité/cohérence, ce checker évalue la fonction narrative.

## Format de sortie
```
PROBLEME: [défaut ou axe d'amélioration]
...
SCORE: N  (entier 1-10)
```

## Échelle de notation
| Note | Signification |
|------|---------------|
| 10 | Objectif pleinement couvert |
| 8-9 | Très bien couvert |
| 6-7 | Objectif couvert mais plusieurs séquences à renforcer |
| 4-5 | Insuffisant — objectif secondaire |
| 1-3 | Mauvais / Inutilisable |

## Parsing
Utilise `ProblemScoreParser` (format PROBLEME:/SCORE:).

## Différence avec les critics
- Critics → qualité/cohérence (AMELIORATION/DEFAUT tiers)
- GoalPlanChecker → fonction narrative (PROBLEME/SCORE)

## Source Redacteur
`story.context.NarrativeGoalContext.evaluatePlan`
