# StoryCompressor

## Rôle
Produit un résumé factuel cumulatif de l'histoire après chaque chapitre.
Ce résumé est injecté dans les chapitres suivants comme "Histoire jusqu'ici".

## Croissance du résumé
La taille cible augmente avec le nombre de chapitres :
```
maxWords = summaryBaseWords + chapterIndex × summaryWordsPerChapter
```

## Ce qu'il inclut
- Événements et actions qui ont eu lieu (faits séquentiels)
- État actuel des personnages (où ils sont, ce qu'ils savent, leurs décisions)
- Changements de relation, d'alliance, de statut
- Objets, lieux, éléments concrets pour la continuité

## Ce qu'il exclut
- Descriptions atmosphériques (sauf signification narrative directe)
- Ressenti émotionnel interne (sauf décision ou tournant dramatique)
- Répétitions d'informations déjà dans le résumé existant

## Gestion du contexte pour les longs livres
`existingSummary` est tronqué aux dernières phrases (jusqu'à `contextWindow/6` chars).
Le nouveau chapitre est tronqué à `contextWindow/2` chars.

## Source Redacteur
`story.context.CompressorContext`
