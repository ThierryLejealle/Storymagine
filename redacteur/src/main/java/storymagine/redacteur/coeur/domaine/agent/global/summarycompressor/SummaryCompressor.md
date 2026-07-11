# SummaryCompressor

## Rôle
Condense le résumé global de l'histoire (`Story.summary`) à environ la moitié de sa taille,
déclenché uniquement quand ce résumé dépasse le seuil de budget. Contrairement à
`ChapterSummarizer`, il ne travaille jamais sur un seul chapitre — son entrée est déjà une
concaténation de plusieurs résumés de chapitre indépendants, potentiellement contradictoires
(un fait qui a évolué d'un chapitre à l'autre) : c'est la seule vraie différence de mandat avec
`ChapterSummarizer`, le reste du prompt est volontairement très proche.

## Déclenchement
Après l'ajout du résumé du chapitre courant (`ChapterSummaryStep`) : si
`nbMots(Story.summary) > SummaryBudget.wordBudget(contextWindow)` (même seuil, en mots, que le
budget `summary` du Writer et du ChapterPlanner — voir `SummaryBudget`), `SummaryCompressor` est appelé et son
résultat remplace `Story.summary` en entier (`Story.compressSummary`), ce qui double aussi
`Story.chapterSummaryDivisor` pour les chapitres suivants.

## Budget
```
motsCible = nbMots(résumé actuel) / 2
```

## Ce qu'il inclut
- Événements et actions marquants (faits datés ou séquentiels)
- État actuel des personnages principaux
- Changements de relation, d'alliance, de statut **toujours valables aujourd'hui**
- Objets, lieux, éléments concrets importants pour la continuité

## Ce qu'il exclut
- Descriptions atmosphériques, ressenti émotionnel ponctuel
- Un fait devenu obsolète : si un développement plus récent l'a remplacé, ne garde que l'état
  le plus récent (arbitrage entre chapitres — le vrai rôle spécifique de cet agent)

## Source Redacteur
`ChapterSummaryStep`, appelé depuis `EvaluateWorkflow` quand le seuil est dépassé.
