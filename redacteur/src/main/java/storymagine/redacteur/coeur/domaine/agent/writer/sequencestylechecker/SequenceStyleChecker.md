# SequenceStyleChecker

## Rôle
Évalue la qualité stylistique d'une séquence unique. Notateur strict — réserve les notes élevées à la vraie qualité.

## Défauts détectés
- Verbes faibles ou abstraits
- Constructions nominalisées ou passives inutiles
- Répétitions de structure dans le même passage
- Formules génériques ou clichés ("un sourire triste", "le cœur lourd")
- Adjectifs de remplissage
- Transitions mécaniques ou coutures visibles
- Phrases qui sonnent fabriquées

## Échelle
| Note | Signification |
|------|---------------|
| 10 | Texte publiable tel quel — irréprochable |
| 8 | Bon texte, défauts mineurs |
| 7 | Correct mais plat |
| 6 | Problèmes qui nuisent à la lecture |
| 5 | Plusieurs défauts sérieux — réécriture partielle |
| 3 | À réécrire intégralement |
| 1 | Trahit visiblement sa fabrication |

## Format de sortie
```
PROBLEME: [description courte et précise]
SCORE: N
```

## Interaction avec le styleGuide
Si `styleGuide` est fourni, les défauts qu'il prescrit explicitement ne sont pas signalés.

## Slots
- text : 50% du contexte
- styleGuide / qualityCriteria : 1/8 chacun
- writingExample : 1/6

## Source Redacteur
`story.context.SequenceStyleCheckerContext`
