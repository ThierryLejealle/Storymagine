# 2026-06-23 21h00 - Fix : nom des personnages absent du prompt ChapterPlanner

## Évolution demandée
Le log LLM du ChapterPlanner montrait les blocs `## Privé` / `## Inconscient` sans aucun nom de personnage.
Le LLM planifiait le chapitre sans savoir à qui appartenaient les traits de caractère.

## Ce qui a été touché
- `ScenarioFormatters.personnages()` — ajout d'un en-tête `#### <Nom>` avant chaque bloc personnage
- `ScenarioFormatters` — ajout de `formatPersonnageName()` (id snake_case → titre capitalisé) et `shiftHeadings()` (décalage `## ` → `##### ` pour respecter la hiérarchie Markdown dans le prompt)
- Import `java.util.Arrays` ajouté

## Hiérarchie Markdown résultante dans le prompt
```
### Personnages de ce chapitre
#### Eddie
##### Privé
Il n'attend rien de ce voyage.
##### Inconscient
...
#### Maya
##### Privé
...
```

## Résultat
Le LLM reçoit désormais le nom de chaque personnage clairement identifié avant ses traits, ce qui lui permet de les attribuer correctement dans le plan.
