# 2026-10-27 - TODO : trois idées chat notées (crochets, lore, delta de fiche)

## 1. Évolution demandée

Noter dans le TODO trois idées à ne pas perdre : un autre caractère pour les commandes que `[...]`,
des fiches « lore » importables, une commande qui ajoute une consigne à la fiche d'un personnage.

## 2. Ce qui a été touché

- `specs/todo.md` : nouvelle section « Chat - idées à ne pas perdre ». Aucun code ni prompt modifié.

Suite de la discussion : l'idée du delta de fiche devient deux commandes, `DELTA` (partie publique)
et `SECRET` (partie privée), écrites dans les actes. Les deltas se recalculent depuis les actes
jusqu'à l'acte courant (aucun stockage dans le run), les lignes sont à retirer du texte d'acte partagé.

Suite : grammaire unique des commandes `{commande sous-type argument : texte}` (sous-type obligatoire en
2e mot, argument en 3e), numérotation des idées 1 à 3, ajout de l'idée 4 (fonction de validation
statique du scénario). Les scénarios existants seront migrés par un outil, sans compatibilité de syntaxe.

## 3. Résultat

Idées consignées, avec les points ouverts du delta de fiche (cible SECRET ou PUBLIC, enregistrement
dans le run, moment de déclenchement, budget de contexte).
