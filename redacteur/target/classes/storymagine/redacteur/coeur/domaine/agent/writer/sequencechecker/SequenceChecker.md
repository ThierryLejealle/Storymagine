# SequenceChecker

## Rôle
Vérifie qu'une séquence rédigée contient tous les éléments requis listés dans `checks`.
Évalue la présence, pas la qualité. N'est appelé que si `checks` est non vide.

## Seuil de présence
Un élément est présent seulement s'il est développé dans au moins une phrase qui le traite directement.
Une allusion fugace ou une mention en passant **ne compte pas**.

## Format de sortie
```
MANQUANT: [élément] — absent
MANQUANT: [élément] — présent mais non développé
SCORE: N  (10 = tous présents ; -1 pt par élément manquant ou insuffisant)
```

## Slots
- sequenceText : 1/3 du contexte
- sequenceDescription : non tronqué (court)

## Source Redacteur
`story.context.SequenceCheckerContext`
