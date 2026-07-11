# ChapterDreamCritic

## Rôle
Évalue la qualité onirique d'un chapitre de rêve.
N'évalue ni les lois physiques, ni la vraisemblance historique, ni la cohérence avec la trame principale.
Calibré selon le niveau de réalisme déclaré.

## Niveaux de réalisme
| Niveau | Critères d'évaluation |
|--------|----------------------|
| `symbolic` (défaut) | Puissance symbolique (archétypes, métaphores), résonance émotionnelle, cohérence interne |
| `realistic` | Intensité émotionnelle, images familières légèrement décalées, résonance psychologique |
| `surreal` | Originalité radicale, logique interne absurde cohérente, puissance sensorielle/émotionnelle |

## Format de sortie
```
PROBLEME: [défaut ou axe d'amélioration]
SCORE: N  (entier 0-10)
```

## Note : pas de AMELIORATION/DEFAUT tiers
Contrairement aux critics principaux, ce critic utilise le format PROBLEME/SCORE
(pas de score tiered — `ProblemScoreParser`, pas `CriticOutputParser`).

## Source Redacteur
`story.context.CriticContext.evalDreamQuality`
