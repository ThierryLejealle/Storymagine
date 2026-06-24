# GoalTextChecker

## Rôle
Évalue si un texte de chapitre produit l'effet narratif ou émotionnel requis par son objectif.
Distinct des critics : les critics évaluent la qualité/cohérence, ce checker évalue la fonction narrative.

## Format de sortie
```
PROBLEME: [défaut ou axe d'amélioration]
SCORE: N  (entier 0-10)
```

## Échelle de notation
| Note | Signification |
|------|---------------|
| 10 | Objectif pleinement atteint |
| 8-9 | Bien atteint |
| 6-7 | Partiellement atteint |
| 5 | Mal atteint |
| 3 | À réécrire : rate l'objectif |

## Différence avec les critics
- Critics → qualité/cohérence structurelle (AMELIORATION/DEFAUT tiers)
- GoalTextChecker → effet narratif/émotionnel (PROBLEME/SCORE)

## Source Redacteur
`story.context.NarrativeGoalContext.evaluateText`
