# RepetitionTracker

## Rôle
Extrait du texte deux listes glissantes d'éléments à éviter dans les séquences suivantes :
- **EXPRESSIONS** : tournures lexicales distinctives à ne pas répéter verbatim
- **SCHÉMAS** : patterns narratifs récurrents décrits de façon abstraite

## Contraintes de sélection
- EXPRESSIONS : 3-8 entrées, minimum 3 mots, pas de redite avec `alreadyPhrases`
- SCHÉMAS : 2-5 entrées, description neutre du concept (pas la formulation exacte), pas de redite avec `alreadyThemes`

## Format de sortie
```
EXPRESSIONS:
- expression 1
- expression 2

SCHÉMAS:
- schéma 1
```

## Validation de parsing
Lignes acceptées : commençant par `-`, `–`, `•` après la section header.
Filtrage : longueur entre 5 et 120 caractères.

## Limitation de taille
Le texte est tronqué à 10 000 caractères (env. 2 500 tokens).

## Source Redacteur
`story.context.RepetitionTrackerContext`
