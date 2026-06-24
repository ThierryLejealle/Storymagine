# FocusActionFilter

## Rôle
Sélectionne par LLM quels groupes de focus et catégories d'action sont pertinents
pour la séquence courante. Évite d'injecter des ressources non pertinentes dans le Writer.

## Format de sortie
```
FOCUS: NOM_GROUPE1, NOM_GROUPE2
ACTIONS: cat1, cat2
```
Listes vides autorisées si rien n'est pertinent.

## Validation
Seuls les noms présents dans `groupNames` / `actionCategories` sont acceptés (filtrés en Java après parsing).
Les noms sont comparés case-insensitive.

## Fallback
Si le LLM omet entièrement une ligne (réponse malformée), la liste complète est utilisée en fallback.
Si le LLM retourne une ligne vide (liste explicite vide), ce vide est respecté.

## Répétition
Si `avoidRepetition=true` et `alreadyUsed` non vide, les groupes déjà utilisés sont mentionnés
dans le prompt pour encourager la variété.

## Source Redacteur
`story.context.LlmFilterContext`
