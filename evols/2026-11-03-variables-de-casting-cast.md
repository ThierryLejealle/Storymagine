# 2026-11-03 - Variables de casting : `{cast}`, `%NOM%`, `{repeat … cast}` et `{repeat N}`

## Évolution demandée
Pouvoir faire choisir un personnage par le scénario (au hasard, sans doublon, y compris dans une boucle) et le
désigner ensuite par une variable. Cas de test voulu : 3 PNJ, un `repeat` sur chaque PNJ, et **en plus** un autre
PNJ tiré, jamais celui de la boucle. Décisions prises avec l'auteur :
- Commande `{cast NOM [unique] [reuse-possible] [(jack, mia, john)]}`, seule sur sa ligne, qui ne s'affiche pas.
  La liste est facultative (sans elle, tout le casting) ; **un seul mot par personnage**.
- Variable `%NOM%` (nom : une lettre puis lettres, chiffres ou `_`), utilisable dans le texte et dans les commandes
  (`{npc-starts %PNJ_1%}`).
- **Exclusif par défaut** : un `cast` ne reprend pas un personnage que tiennent les autres variables. Le mot clé
  `reuse-possible` lève cette règle. `unique` : la variable ne reprend jamais un personnage qu'elle a déjà eu
  (dans un `repeat`, un personnage différent par copie), relatif à la variable.
- `{repeat order|random [N] cast NOM [(liste)]}` : une copie par personnage ; `{repeat N}` : N copies sans fichier.
- Les variables déclarées dans une copie sont oubliées à sa fin ; celles d'avant le `repeat` restent et sont exclues.
- La validation doit vérifier la liste et signaler, au mieux, une combinaison impossible.
- Deux sujets indépendants : les variables ici, les bibliothèques communes `/lib/` dans la fiche suivante.

## Ce qui a été touché
- Domaine `pick` : `CastCommand` (syntaxe), `CastPool` (liste ou casting, inconnu / ambigu / doublon = erreur),
  `CastResolver` (tirage par retour en arrière, état immuable, budget de 200 000 appels), `PickMode.CAST`.
  `RepeatExpander` (formes `repeat N` et `repeat … cast`, lignes internes autour des copies qui portent un cast),
  `PickResolver` (les casts s'exécutent après les `pick`, donc un `%NOM%` dans un paragraphe tiré est remplacé).
  `Cast.findAllByName`.
- Validation : `ScenarioValidator` (`cast` connu, contrôle des listes, `%NOM%` déclaré plus haut et oublié à la fin
  du bloc `repeat`, ligne contrôlée avec un personnage à la place de la variable, résolution complète pour trouver
  une combinaison impossible, `{npc-story}` avec variable sans contrôle du tag avant le tirage).
- Infra : `ChatFileStorageAdapter` passe le casting au dépliage et à la validation.
- Démo : `demo-cast` (atelier de cuisine : un chef par recette, un commis toujours différent, un pâtissier tiré dans
  une liste).
- Tests : `CastCommandTest`, `CastResolutionTest` (exclusivité, `reuse-possible`, liste, retour en arrière,
  combinaison impossible, `unique` dans un repeat, `repeat cast`, variables de copie, variable dans un paragraphe
  tiré, tirages enregistrés), `CastScenarioLoadingTest` (scénario fichier : l'ouverture vise le chef, le commis est
  un autre, tirage figé pour la partie, validation sans run), `ScenarioValidatorTest`, `DemoScenariosTest` (13 démos).
- Documentation : guide §2.6 bis (nouvelles formes de `repeat`), §2.6 ter (nouveau), §2.9 ; skill
  `chat-scenario-writing` ; `specs/chat.md`.
- Aucun prompt d'agent modifié.

## Résultat
Tests verts. Le cas de test (chef de la boucle + un autre PNJ) s'écrit sans mot clé : `{cast COMMIS}` dans un
`{repeat order cast CHEF}` n'est jamais le chef. Une combinaison impossible est détectée à la validation et au
chargement (recherche complète, pas un simple essai), avec le nom de la commande.
**Non vérifié à la main** : le tableau 🎲 dans l'interface avec les lignes `cast` et `repeat-order`, et le rendu d'une
partie réelle avec un LLM.
**Limites connues** : la valeur d'une variable est le nom complet du personnage ; un `%NOM%` écrit hors d'un
scénario résolu (par exemple dans `style.txt`) n'est pas remplacé ; au-delà de 200 000 combinaisons essayées, le
chargement échoue en demandant de simplifier (cas très improbable avec un casting de quelques personnages).
