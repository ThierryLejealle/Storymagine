# ChapterSummarizer

## Rôle
Produit un résumé factuel indépendant d'UN chapitre, écrit une seule fois et jamais retouché
ensuite. Rejoint le résumé global de l'histoire (`Story.summary`) par simple concaténation
(`Story.appendChapterSummary`).

## Budget
```
motsCible = motsDuChapitre / divisor
```
`divisor` vient de `Story.chapterSummaryDivisor` (démarre à 5, double à chaque compaction
déclenchée par `SummaryCompressor` — voir `SummaryCompressor.md`). Pas de plancher : un livre
très long avec un diviseur devenu grand produira des résumés de chapitre très courts — accepté
comme limite connue, pas un bug (voir mémoire "budget Writer trop timide ?" pour le contexte de
la discussion, chantier explicitement pas suivi pour l'instant).

## Ce qu'il inclut
- Événements et actions qui ont eu lieu (faits datés ou séquentiels)
- État des personnages à la fin du chapitre (où ils sont, ce qu'ils savent, leurs décisions)
- Changements de relation, d'alliance, de statut
- Objets, lieux, éléments concrets importants pour la suite

## Ce qu'il exclut
- Descriptions atmosphériques (sauf signification narrative directe)
- Ressenti émotionnel interne ponctuel

## Pourquoi indépendant (pas cumulatif)
L'ancienne version (`StoryCompressor`) refondait l'ancien résumé + le nouveau chapitre en un
seul bloc à chaque fois, capé à une taille qui ne grandissait que de 200 mots/chapitre — les faits
des premiers chapitres n'avaient aucune protection et pouvaient disparaître silencieusement au
fil des repasses. `ChapterSummarizer` écrit un résumé définitif par chapitre : la seule perte de
détail acceptée se produit désormais lors d'une compaction explicite et rare (`SummaryCompressor`),
pas à chaque chapitre.

## Source Redacteur
`ChapterSummaryStep`, appelé depuis `EvaluateWorkflow` après validation de chaque chapitre.
