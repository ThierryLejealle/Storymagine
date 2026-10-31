# 2026-10-31 - Commande repeat : une séquence de scénario écrite une fois, répétée par élément

## Évolution demandée
Une variante de `pick` pour une série de séquences qui font toutes la même chose (par exemple une suite
d'épreuves). Le gabarit s'écrit une fois ; il est répété pour chaque paragraphe d'un fichier de `picks/`.
Une seule zone change, avec un numéro d'instance. Décisions prises avec l'auteur :
- Syntaxe `{repeat order|random [N] nom}` … `{end-repeat}`, sans N on déroule tout le fichier.
- `order` = ordre du fichier, `random` = ordre aléatoire ; « unique » dans le contexte d'un seul bloc
  (deux blocs sur le même fichier sont indépendants).
- Zones `{n}` (numéro), `{item}` (paragraphe entier) et `{item1}`, `{item2}`… (morceaux d'un paragraphe,
  séparés par une ligne ne contenant que `-`).
- Erreur si N dépasse la taille du fichier, ou si un paragraphe n'a pas le bon nombre de morceaux.
- Un `{pick}` est autorisé dans un gabarit. Le dépliage est un pré-traitement : le scénario du run
  (`resolved/`) ne contient plus de `repeat`.
- Fichiers dans `picks/`. Aucune règle particulière pour les lignes `-` lues par un `{pick}` ordinaire.
- Scénario de test minimaliste (devenu `demos/demo-repeat`, voir la fiche suivante).

## Ce qui a été touché
- Domaine `coeur/domaine/pick/` : nouveaux `RepeatExpander`, `RepeatMode`, `RepeatExpansion`,
  `PickParagraph`, `RandomSelection` (tirage partagé avec `PickResolver`). `PickMode` : deux types
  enregistrés (`repeat-order`, `repeat-random`) et `parseRecorded`. `PickResolver` déplie les blocs avant de
  résoudre les `pick`, et `containsTag` reconnaît aussi `repeat`.
- `ScenarioValidator` : `repeat`, `end-repeat`, `n`, `item` sont des commandes connues ; les blocs sont
  validés fichier par fichier.
- Infra : `PickDrawsFile` lit les nouveaux types ; `PickFileLibrary` nomme le fichier dans ses messages
  d'erreur sans parler de `pick`.
- Tests : `RepeatExpanderTest` (nouveau), `ScenarioValidatorTest` et `ChatFileStoragePicksTest` complétés.
- Scénario de test `chatscenarios/demos/demo-repeat/` (5 tâches dans `picks/taches.txt`, 3 jouées au hasard).
- Documentation : guide §2.6 bis et §2.9, skill `chat-scenario-writing`, `specs/chat.md`.
- Hors périmètre, nécessaire pour compiler : `ChatWebServer` contenait deux copies identiques de
  `handleContinue` (modification précédente non commitée) ; la seconde a été supprimée.
- Aucun prompt d'agent modifié.

## Résultat
Un scénario peut déclarer une séquence une seule fois et la jouer N fois avec un paragraphe différent
chaque fois. `demo-repeat` se valide sans erreur et se déplie en 5 actes (introduction, 3
tâches, pause). Tests unitaires et tests du module `chat` verts.
