# 2026-07-13 18h12 - Chat : scenario en fichier unique structure (headings imbriques) au lieu d'un fichier par acte

## Demande

Le decoupage d'un scenario en un fichier `act-N.txt` par acte etait juge peu pratique a gerer
("note pour apres : le decoupage en act par fichier c'est peu pratique a gerer"). Demande de
revenir a un seul fichier `scenario.txt`, mais STRUCTURE avec des titres Markdown imbriques
(`#`, `##`, `###`...), avec heritage du contenu du parent vers les enfants, et les "beats"
(lignes `[...]`) d'un parent qui ne se declenchent qu'une seule fois, a la premiere entree dans
sa branche.

## Ce qui a ete touche

- `chat/infra/ScenarioOutlineParser.java` (nouveau/reecrit) : parse un `scenario.txt` unique en
  arbre de titres Markdown, aplati en liste de `ScenarioAct` feuilles. Le texte resolu d'une
  feuille = concatenation des corps de tous ses ancetres + le sien. Les beats d'un ancetre ne sont
  emis que pour la premiere feuille qui entre dans sa branche (diff de chemin par rapport a la
  feuille precedente).
- `ScenarioAct.java` : ajout du champ `beats` (`List<String>`) et d'une factory `leaf(...)`.
- `ChatFileStorageAdapter.java` : `loadScenario` reecrit pour lire le fichier unique ; ajout de
  `archiveFoldedTurns` (archive les tours compactes dans un sous-dossier `archive/` horodate) ;
  `resetSession` passe par ce meme mecanisme d'archivage.
- `ChatStoragePort.java` : ajout de `archiveFoldedTurns`.
- `ChatFileStorageAdapterTest.java` : anciens helpers `writeActFile`/fichiers `act-N.txt`
  remplaces par `writeActs(...)`/`appendActs(...)` ecrivant dans le fichier unique.
- Reformulation clarifiee aupres de l'utilisateur : le LLM ne recoit jamais de marqueur explicite
  "CURRENT ACT" — seul le texte resolu de l'acte courant lui est fourni, sans metadonnee de
  position (correction apportee suite a une question de l'utilisateur : "Pourquoi tu dis current
  act au LLM ? Ca lui sert a quoi ?").

## Resultat

`mvn -pl chat test` complet au vert apres la migration (tous les sites d'appel de l'ancien
format `int` de numero d'acte et de l'ancienne API de stockage mis a jour). Le scenario
`temple-noir-actes` a servi de premier cas d'usage reel du nouveau format (voir fiche suivante).
