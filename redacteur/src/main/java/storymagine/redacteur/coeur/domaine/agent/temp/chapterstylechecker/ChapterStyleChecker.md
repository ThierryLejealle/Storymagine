# ChapterStyleChecker

## Rôle
Évalue la qualité stylistique de l'intégralité du texte d'un chapitre terminé.
Équivalent chapitre de `SequenceStyleChecker`; s'applique sur le texte complet agrégé.

## Défauts détectés
(identiques à SequenceStyleChecker)
- Verbes faibles ou abstraits
- Constructions nominalisées ou passives inutiles
- Répétitions de structure dans le même passage
- Formules génériques ou clichés de style
- Adjectifs de remplissage
- Transitions mécaniques ou coutures visibles
- Phrases qui sonnent fabriquées

## Échelle de notation
| Note | Signification |
|------|---------------|
| 10 | Parfait, rien à retoucher |
| 9 | Excellent |
| 8 | Bon |
| 7 | Lisible mais plusieurs maladresses |
| 6 | Correct mais largement améliorable |
| 5 | Moyen |
| 3 | À réécrire intégralement |

## Format de sortie
```
PROBLEME: [défaut ou axe d'amélioration]
SCORE: N
```

## Interaction avec le styleGuide
Si `styleGuide` est fourni, les défauts qu'il prescrit explicitement ne sont pas signalés.

## Slots
- text : 55% du contexte
- styleGuide / qualityCriteria : 1/8 chacun
- writingExample : 1/6

## Source Redacteur
`story.context.ChapterStyleCheckerContext`
