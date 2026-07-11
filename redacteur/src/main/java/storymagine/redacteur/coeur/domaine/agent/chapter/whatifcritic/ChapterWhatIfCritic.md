# ChapterWhatIfCritic

## Rôle
Évalue la plausibilité physique et causale d'un chapitre what-if.
N'évalue pas la cohérence avec la trame principale — la divergence est intentionnelle.

## Critères d'évaluation
- Les conséquences découlent-elles logiquement de la prémisse ?
- Les lois physiques et la réalité matérielle sont-elles cohérentes dans ce monde alternatif ?
- Les personnages sont-ils psychologiquement crédibles face à la nouvelle situation ?

## Format de sortie
```
PROBLEME: [défaut ou axe d'amélioration]
SCORE: N  (entier 0-10)
```

## Échelle de notation
| Note | Signification |
|------|---------------|
| 10 | Hypothèse parfaitement plausible, causalité solide |
| 7-9 | Globalement plausible avec imperfections mineures |
| 5-6 | Incohérences causales qui fragilisent la suspension d'incrédulité |
| 3 | Logique causale fondamentalement défaillante |

## Note : format PROBLEME/SCORE (pas tiered)
Utilise `ProblemScoreParser`, pas `CriticOutputParser`.

## Source Redacteur
`story.context.CriticContext.evalWhatIfPlausibility`
